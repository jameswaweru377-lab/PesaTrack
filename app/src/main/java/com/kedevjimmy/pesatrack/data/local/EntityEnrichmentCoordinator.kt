package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.data.remote.EntityEnrichmentHttpClient
import com.kedevjimmy.pesatrack.domain.intelligence.EntityDescriptorPrivacyPolicy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class EntityEnrichmentCoordinator private constructor(
    context: Context
) {
    private val appContext =
        context.applicationContext
    private val cache =
        EntityEnrichmentCacheStore.get(
            appContext
        )
    private val remote =
        EntityEnrichmentHttpClient()
    private val executor =
        Executors.newSingleThreadExecutor()
    private val inFlight =
        ConcurrentHashMap.newKeySet<Long>()

    fun enrichTransactionAsync(
        transactionId: Long,
        rawDescriptor: String?
    ) {
        if (!remote.isConfigured()) {
            return
        }

        val sanitized =
            EntityDescriptorPrivacyPolicy
                .sanitize(rawDescriptor)
                ?: return
        if (!inFlight.add(transactionId)) {
            return
        }
        val key =
            EntityDescriptorPrivacyPolicy
                .cacheKey(sanitized)

        executor.execute {
            try {
                val cached = cache.get(key)
                if (cached != null) {
                    TransactionStore.get(appContext)
                        .applyRemoteEntityEnrichment(
                            transactionId =
                                transactionId,
                            result = cached
                        )
                    return@execute
                }

                val resolved =
                    remote.enrich(sanitized)
                        ?: return@execute

                cache.put(
                    descriptorKey = key,
                    result = resolved
                )

                TransactionStore.get(appContext)
                    .applyRemoteEntityEnrichment(
                        transactionId =
                            transactionId,
                        result = resolved
                    )
            } finally {
                inFlight.remove(transactionId)
            }
        }
    }

    companion object {
        @Volatile
        private var instance:
            EntityEnrichmentCoordinator? = null

        fun get(
            context: Context
        ): EntityEnrichmentCoordinator =
            instance ?: synchronized(this) {
                instance
                    ?: EntityEnrichmentCoordinator(
                        context.applicationContext
                    ).also {
                        instance = it
                    }
            }
    }
}

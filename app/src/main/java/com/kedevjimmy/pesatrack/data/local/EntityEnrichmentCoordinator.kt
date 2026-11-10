package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kedevjimmy.pesatrack.data.remote.EntityEnrichmentClientIdentity
import com.kedevjimmy.pesatrack.data.remote.EntityEnrichmentFetchResult
import com.kedevjimmy.pesatrack.data.remote.EntityEnrichmentHttpClient
import com.kedevjimmy.pesatrack.domain.intelligence.EntityDescriptorPrivacyPolicy
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

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
    private val preferences =
        EntityEnrichmentPreferences.get(
            appContext
        )

    @Suppress("UNUSED_PARAMETER")
    fun enrichTransactionAsync(
        transactionId: Long,
        rawDescriptor: String?
    ) {
        // Strict offline mode: legacy online-enrichment settings cannot
        // initiate a request. Existing locally cached labels are retained.
    }

    private fun uniqueWorkName(
        descriptorKey: String
    ): String {
        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )
                .digest(
                    descriptorKey
                        .toByteArray(
                            Charsets.UTF_8
                        )
                )
                .take(8)
                .joinToString("") {
                    "%02x".format(it)
                }

        return "entity-enrichment-$digest"
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

class EntityEnrichmentWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(
    appContext,
    workerParams
) {
    override fun doWork(): Result {
        // Ignore old queued remote jobs following the strictly offline update.
        return Result.success()
        val sanitized =
            inputData.getString(
                KEY_DESCRIPTOR
            )
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: return Result.failure()

        val verified =
            EntityDescriptorPrivacyPolicy
                .sanitize(sanitized)
                ?: return Result.failure()
        if (verified != sanitized) {
            return Result.failure()
        }

        val key =
            EntityDescriptorPrivacyPolicy
                .cacheKey(verified)
        val cache =
            EntityEnrichmentCacheStore.get(
                applicationContext
            )

        cache.get(key)?.let { cached ->
            TransactionStore
                .get(applicationContext)
                .applyRemoteEntityEnrichmentByDescriptor(
                    sanitizedDescriptor =
                        verified,
                    result = cached
                )
            return Result.success()
        }

        if (
            !EntityEnrichmentPreferences
                .get(applicationContext)
                .isOnlineLookupEnabled()
        ) {
            return Result.success()
        }

        val remote =
            EntityEnrichmentHttpClient(
                clientId =
                    EntityEnrichmentClientIdentity
                        .get(applicationContext)
            )
        if (!remote.isConfigured()) {
            return Result.success()
        }

        return when (
            val response =
                remote.fetch(verified)
        ) {
            is EntityEnrichmentFetchResult
                .Resolved -> {
                cache.put(
                    descriptorKey = key,
                    result = response.value
                )
                TransactionStore
                    .get(applicationContext)
                    .applyRemoteEntityEnrichmentByDescriptor(
                        sanitizedDescriptor =
                            verified,
                        result =
                            response.value
                    )
                Result.success()
            }

            EntityEnrichmentFetchResult
                .NotFound ->
                Result.success()

            is EntityEnrichmentFetchResult
                .Retryable ->
                Result.retry()

            is EntityEnrichmentFetchResult
                .PermanentFailure ->
                Result.failure()
        }
    }

    companion object {
        const val KEY_DESCRIPTOR =
            "descriptor"
        const val KEY_TRIGGER_TRANSACTION_ID =
            "trigger_transaction_id"
    }
}

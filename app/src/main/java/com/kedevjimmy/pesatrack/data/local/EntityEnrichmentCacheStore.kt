package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.intelligence.EntityMatchConfidence
import com.kedevjimmy.pesatrack.domain.intelligence.RemoteEntityEnrichment
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory

class EntityEnrichmentCacheStore private constructor(
    context: Context
) {
    private val database =
        PesaTrackDatabase.get(context.applicationContext)

    @Synchronized
    fun get(
        descriptorKey: String,
        nowEpochMs: Long =
            System.currentTimeMillis()
    ): RemoteEntityEnrichment? {
        database.writableDatabase.delete(
            "entity_enrichment_cache",
            "expires_at_epoch_ms <= ?",
            arrayOf(nowEpochMs.toString())
        )

        return database.readableDatabase.rawQuery(
            """
            SELECT canonical_name,
                   domain,
                   logo_url,
                   category_hint,
                   confidence,
                   source
            FROM entity_enrichment_cache
            WHERE descriptor_key = ?
              AND expires_at_epoch_ms > ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                descriptorKey,
                nowEpochMs.toString()
            )
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                RemoteEntityEnrichment(
                    canonicalName =
                        cursor.getString(0),
                    domain =
                        if (cursor.isNull(1)) {
                            null
                        } else {
                            cursor.getString(1)
                        },
                    logoUrl =
                        if (cursor.isNull(2)) {
                            null
                        } else {
                            cursor.getString(2)
                        },
                    categoryHint =
                        if (cursor.isNull(3)) {
                            null
                        } else {
                            runCatching {
                                TransactionCategory
                                    .valueOf(
                                        cursor.getString(3)
                                    )
                            }.getOrNull()
                        },
                    confidence =
                        runCatching {
                            EntityMatchConfidence
                                .valueOf(
                                    cursor.getString(4)
                                )
                        }.getOrDefault(
                            EntityMatchConfidence
                                .LIKELY
                        ),
                    source =
                        cursor.getString(5)
                )
            }
        }
    }

    @Synchronized
    fun put(
        descriptorKey: String,
        result: RemoteEntityEnrichment,
        fetchedAtEpochMs: Long =
            System.currentTimeMillis(),
        ttlMs: Long = DEFAULT_TTL_MS
    ) {
        val values = ContentValues().apply {
            put("descriptor_key", descriptorKey)
            put(
                "canonical_name",
                result.canonicalName
            )
            if (result.domain != null) {
                put("domain", result.domain)
            } else {
                putNull("domain")
            }
            if (result.logoUrl != null) {
                put("logo_url", result.logoUrl)
            } else {
                putNull("logo_url")
            }
            if (result.categoryHint != null) {
                put(
                    "category_hint",
                    result.categoryHint.name
                )
            } else {
                putNull("category_hint")
            }
            put(
                "confidence",
                result.confidence.name
            )
            put("source", result.source)
            put(
                "fetched_at_epoch_ms",
                fetchedAtEpochMs
            )
            put(
                "expires_at_epoch_ms",
                fetchedAtEpochMs + ttlMs
            )
        }

        database.writableDatabase
            .insertWithOnConflict(
                "entity_enrichment_cache",
                null,
                values,
                android.database.sqlite
                    .SQLiteDatabase.CONFLICT_REPLACE
            )
    }

    companion object {
        private const val DEFAULT_TTL_MS =
            30L * 24L * 60L * 60L * 1000L

        @Volatile
        private var instance:
            EntityEnrichmentCacheStore? = null

        fun get(
            context: Context
        ): EntityEnrichmentCacheStore =
            instance ?: synchronized(this) {
                instance
                    ?: EntityEnrichmentCacheStore(
                        context.applicationContext
                    ).also {
                        instance = it
                    }
            }
    }
}

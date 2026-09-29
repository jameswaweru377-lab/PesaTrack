package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.intelligence.EntityDescriptorSanitizer
import com.kedevjimmy.pesatrack.domain.intelligence.EntityMatchConfidence
import com.kedevjimmy.pesatrack.domain.intelligence.OnlineEntityProfile
import java.security.MessageDigest
import org.json.JSONObject

class EntityEnrichmentCache private constructor(
    context: Context
) {
    private val preferences =
        context.applicationContext
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )

    fun get(
        descriptor: String,
        nowEpochMs: Long =
            System.currentTimeMillis()
    ): OnlineEntityProfile? {
        val raw =
            preferences.getString(
                cacheKey(descriptor),
                null
            ) ?: return null

        val json =
            runCatching {
                JSONObject(raw)
            }.getOrNull()
                ?: return null

        val expiresAt =
            json.optLong(
                "expires_at_epoch_ms",
                0L
            )
        if (
            expiresAt <= 0L ||
            expiresAt < nowEpochMs
        ) {
            preferences.edit()
                .remove(cacheKey(descriptor))
                .apply()
            return null
        }

        val canonicalName =
            json.optString(
                "canonical_name",
                ""
            ).trim()
        if (canonicalName.isBlank()) {
            return null
        }

        val confidence =
            runCatching {
                EntityMatchConfidence.valueOf(
                    json.optString(
                        "confidence",
                        EntityMatchConfidence
                            .LIKELY
                            .name
                    )
                )
            }.getOrDefault(
                EntityMatchConfidence.LIKELY
            )

        return OnlineEntityProfile(
            canonicalName = canonicalName,
            domain =
                json.optString(
                    "domain",
                    ""
                ).takeIf(String::isNotBlank),
            logoUrl =
                json.optString(
                    "logo_url",
                    ""
                ).takeIf(String::isNotBlank),
            remoteCategory =
                json.optString(
                    "remote_category",
                    ""
                ).takeIf(String::isNotBlank),
            confidence = confidence,
            source =
                json.optString(
                    "source",
                    "remote"
                )
        )
    }

    fun put(
        descriptor: String,
        profile: OnlineEntityProfile,
        nowEpochMs: Long =
            System.currentTimeMillis()
    ) {
        val json =
            JSONObject()
                .put(
                    "canonical_name",
                    profile.canonicalName
                )
                .put(
                    "domain",
                    profile.domain
                )
                .put(
                    "logo_url",
                    profile.logoUrl
                )
                .put(
                    "remote_category",
                    profile.remoteCategory
                )
                .put(
                    "confidence",
                    profile.confidence.name
                )
                .put(
                    "source",
                    profile.source
                )
                .put(
                    "expires_at_epoch_ms",
                    nowEpochMs + CACHE_TTL_MS
                )

        preferences.edit()
            .putString(
                cacheKey(descriptor),
                json.toString()
            )
            .apply()
    }

    private fun cacheKey(
        descriptor: String
    ): String {
        val normalized =
            EntityDescriptorSanitizer.key(
                descriptor
            )
        val digest =
            MessageDigest.getInstance("SHA-256")
                .digest(
                    normalized.toByteArray(
                        Charsets.UTF_8
                    )
                )
                .joinToString("") {
                    "%02x".format(it)
                }

        return "profile:$digest"
    }

    companion object {
        private const val PREFERENCES_NAME =
            "entity_enrichment_cache"
        private const val CACHE_TTL_MS =
            30L * 24L * 60L * 60L * 1000L

        @Volatile
        private var instance:
            EntityEnrichmentCache? = null

        fun get(
            context: Context
        ): EntityEnrichmentCache =
            instance ?: synchronized(this) {
                instance
                    ?: EntityEnrichmentCache(
                        context
                            .applicationContext
                    ).also {
                        instance = it
                    }
            }
    }
}

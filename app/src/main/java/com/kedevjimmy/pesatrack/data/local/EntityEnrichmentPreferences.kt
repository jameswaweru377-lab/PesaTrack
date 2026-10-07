package com.kedevjimmy.pesatrack.data.local

import android.content.Context

class EntityEnrichmentPreferences private constructor(
    context: Context
) {
    private val preferences =
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

    fun isOnlineLookupEnabled(): Boolean =
        preferences.getBoolean(
            KEY_ONLINE_LOOKUP_ENABLED,
            false
        )

    fun setOnlineLookupEnabled(
        enabled: Boolean
    ) {
        preferences.edit()
            .putBoolean(
                KEY_ONLINE_LOOKUP_ENABLED,
                enabled
            )
            .apply()
    }

    companion object {
        @Volatile
        private var instance:
            EntityEnrichmentPreferences? = null

        fun get(
            context: Context
        ): EntityEnrichmentPreferences =
            instance ?: synchronized(this) {
                instance
                    ?: EntityEnrichmentPreferences(
                        context.applicationContext
                    ).also {
                        instance = it
                    }
            }

        private const val PREFS_NAME =
            "entity_enrichment_preferences"
        private const val KEY_ONLINE_LOOKUP_ENABLED =
            "online_lookup_enabled"
    }
}

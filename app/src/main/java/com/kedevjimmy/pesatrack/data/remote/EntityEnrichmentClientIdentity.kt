package com.kedevjimmy.pesatrack.data.remote

import android.content.Context
import java.util.UUID

internal object EntityEnrichmentClientIdentity {
    private val CLIENT_ID_REGEX =
        Regex(
            """^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$"""
        )

    @Synchronized
    fun get(context: Context): String {
        val preferences =
            context.applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

        preferences
            .getString(KEY_CLIENT_ID, null)
            ?.trim()
            ?.takeIf(CLIENT_ID_REGEX::matches)
            ?.let {
                return it
            }

        val generated =
            UUID.randomUUID().toString()

        preferences.edit()
            .putString(
                KEY_CLIENT_ID,
                generated
            )
            .apply()

        return generated
    }

    private const val PREFS_NAME =
        "entity_enrichment_client_v1"
    private const val KEY_CLIENT_ID =
        "client_id"
}

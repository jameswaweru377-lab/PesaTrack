package com.kedevjimmy.pesatrack.ingestion

import java.security.MessageDigest
import java.util.ArrayDeque

class RecentFinancialNotificationDeduper(
    private val windowMs: Long =
        DEFAULT_WINDOW_MS,
    private val maxEntries: Int =
        DEFAULT_MAX_ENTRIES
) {
    private data class Entry(
        val fingerprint: String,
        val acceptedAtEpochMs: Long
    )

    private val entries =
        ArrayDeque<Entry>()

    @Synchronized
    fun shouldProcess(
        notificationKey: String?,
        sourcePackage: String,
        title: String?,
        body: String,
        observedAtEpochMs: Long
    ): Boolean {
        prune(
            observedAtEpochMs
        )

        val fingerprint =
            fingerprint(
                notificationKey =
                    notificationKey,
                sourcePackage =
                    sourcePackage,
                title = title,
                body = body
            )

        if (
            entries.any {
                it.fingerprint ==
                    fingerprint
            }
        ) {
            return false
        }

        entries.addLast(
            Entry(
                fingerprint =
                    fingerprint,
                acceptedAtEpochMs =
                    observedAtEpochMs
            )
        )

        while (
            entries.size >
            maxEntries
        ) {
            entries.removeFirst()
        }

        return true
    }

    private fun prune(
        nowEpochMs: Long
    ) {
        val cutoff =
            nowEpochMs -
                windowMs

        while (
            entries.firstOrNull()
                ?.acceptedAtEpochMs
                ?.let {
                    it < cutoff
                } == true
        ) {
            entries.removeFirst()
        }
    }

    private fun fingerprint(
        notificationKey: String?,
        sourcePackage: String,
        title: String?,
        body: String
    ): String {
        val normalized =
            buildString {
                append(
                    sourcePackage.trim()
                )
                append('|')
                append(
                    notificationKey
                        ?.trim()
                        .orEmpty()
                )
                append('|')
                append(
                    title
                        ?.trim()
                        ?.lowercase()
                        .orEmpty()
                )
                append('|')
                append(
                    body.trim()
                        .replace(
                            WHITESPACE,
                            " "
                        )
                        .lowercase()
                )
            }

        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    normalized
                        .toByteArray(
                            Charsets.UTF_8
                        )
                )

        return digest.joinToString(
            separator = ""
        ) {
            byte ->
            "%02x".format(
                byte
            )
        }
    }

    companion object {
        const val DEFAULT_WINDOW_MS =
            2L * 60L * 1000L
        private const val
            DEFAULT_MAX_ENTRIES =
            64

        private val WHITESPACE =
            Regex("""\s+""")
    }
}

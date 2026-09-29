package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class FinancialIngestionDiagnosticCode {
    UNRECOGNIZED_FINANCIAL_LANGUAGE,
    UNDERSTOOD_NO_LEDGER_EFFECT,
    OBSERVATION_ONLY,
    PENDING_REVIEW,
    INGESTED
}

data class FinancialIngestionDiagnostic(
    val title: String?,
    val sourceDisplayName: String?,
    val sourceType: String,
    val eventLabel: String,
    val sourcePackage: String?,
    val sourceKind: String,
    val receivedAtEpochMs: Long,
    val code: FinancialIngestionDiagnosticCode,
    val insertedTransactions: Int,
    val insertedFinancialEvents: Int,
    val pendingReviewEvents: Int
)

class FinancialIngestionDiagnosticsStore private constructor(
    context: Context
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    @Synchronized
    fun record(
        title: String?,
        sourceDisplayName: String?,
        sourceType: String,
        eventLabel: String,
        sourcePackage: String?,
        sourceKind: String,
        receivedAtEpochMs: Long,
        financialCandidate: Boolean,
        semanticUnderstood: Boolean,
        result: FinancialMessageIngestionResult
    ) {
        if (!financialCandidate && !result.recognized) {
            return
        }

        val code = when {
            !result.recognized &&
                semanticUnderstood ->
                FinancialIngestionDiagnosticCode
                    .UNDERSTOOD_NO_LEDGER_EFFECT
            !result.recognized ->
                FinancialIngestionDiagnosticCode
                    .UNRECOGNIZED_FINANCIAL_LANGUAGE
            result.pendingReviewEvents > 0 ->
                FinancialIngestionDiagnosticCode
                    .PENDING_REVIEW
            result.insertedTransactions > 0 ||
                result.insertedFinancialEvents > 0 ->
                FinancialIngestionDiagnosticCode.INGESTED
            else ->
                FinancialIngestionDiagnosticCode
                    .OBSERVATION_ONLY
        }

        val current = runCatching {
            JSONArray(
                preferences.getString(
                    KEY_ENTRIES,
                    "[]"
                ) ?: "[]"
            )
        }.getOrElse { JSONArray() }

        val next = JSONArray()
        val start =
            maxOf(0, current.length() - (MAX_ENTRIES - 1))
        for (index in start until current.length()) {
            next.put(current.getJSONObject(index))
        }

        next.put(
            JSONObject().apply {
                put(
                    "title",
                    title
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?.take(MAX_TITLE_LENGTH)
                        ?: JSONObject.NULL
                )
                put(
                    "sourceDisplayName",
                    sourceDisplayName
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?.take(MAX_TITLE_LENGTH)
                        ?: JSONObject.NULL
                )
                put(
                    "sourceType",
                    sourceType
                        .trim()
                        .takeIf(String::isNotBlank)
                        ?.take(MAX_SOURCE_TYPE_LENGTH)
                        ?: "Financial"
                )
                put(
                    "eventLabel",
                    eventLabel
                        .trim()
                        .takeIf(String::isNotBlank)
                        ?.take(MAX_EVENT_LABEL_LENGTH)
                        ?: "Financial message"
                )
                put(
                    "sourcePackage",
                    sourcePackage
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?.take(MAX_PACKAGE_LENGTH)
                        ?: JSONObject.NULL
                )
                put(
                    "sourceKind",
                    sourceKind.take(MAX_SOURCE_KIND_LENGTH)
                )
                put(
                    "receivedAtEpochMs",
                    receivedAtEpochMs
                )
                put("code", code.name)
                put(
                    "insertedTransactions",
                    result.insertedTransactions
                )
                put(
                    "insertedFinancialEvents",
                    result.insertedFinancialEvents
                )
                put(
                    "pendingReviewEvents",
                    result.pendingReviewEvents
                )
            }
        )

        preferences.edit()
            .putString(KEY_ENTRIES, next.toString())
            .apply()
    }

    @Synchronized
    fun recent(
        limit: Int = MAX_ENTRIES
    ): List<FinancialIngestionDiagnostic> {
        val array = runCatching {
            JSONArray(
                preferences.getString(
                    KEY_ENTRIES,
                    "[]"
                ) ?: "[]"
            )
        }.getOrElse { JSONArray() }

        val bounded = limit.coerceIn(1, MAX_ENTRIES)
        val start = maxOf(0, array.length() - bounded)

        return buildList {
            for (
                index in array.length() - 1
                    downTo start
            ) {
                val item = array.getJSONObject(index)
                add(
                    FinancialIngestionDiagnostic(
                        title =
                            if (item.isNull("title")) {
                                null
                            } else {
                                item.optString("title")
                                    .trim()
                                    .takeIf(
                                        String::isNotBlank
                                    )
                            },
                        sourceDisplayName =
                            if (
                                item.isNull(
                                    "sourceDisplayName"
                                )
                            ) {
                                null
                            } else {
                                item.optString(
                                    "sourceDisplayName"
                                ).trim()
                                    .takeIf(
                                        String::isNotBlank
                                    )
                            },
                        sourceType =
                            item.optString(
                                "sourceType",
                                "Financial"
                            ).ifBlank { "Financial" },
                        eventLabel =
                            item.optString(
                                "eventLabel",
                                "Financial message"
                            ).ifBlank {
                                "Financial message"
                            },
                        sourcePackage =
                            if (
                                item.isNull(
                                    "sourcePackage"
                                )
                            ) {
                                null
                            } else {
                                item.optString(
                                    "sourcePackage"
                                ).trim()
                                    .takeIf(
                                        String::isNotBlank
                                    )
                            },
                        sourceKind =
                            item.optString("sourceKind"),
                        receivedAtEpochMs =
                            item.optLong(
                                "receivedAtEpochMs"
                            ),
                        code =
                            runCatching {
                                FinancialIngestionDiagnosticCode
                                    .valueOf(
                                        item.optString(
                                            "code"
                                        )
                                    )
                            }.getOrDefault(
                                FinancialIngestionDiagnosticCode
                                    .UNRECOGNIZED_FINANCIAL_LANGUAGE
                            ),
                        insertedTransactions =
                            item.optInt(
                                "insertedTransactions"
                            ),
                        insertedFinancialEvents =
                            item.optInt(
                                "insertedFinancialEvents"
                            ),
                        pendingReviewEvents =
                            item.optInt(
                                "pendingReviewEvents"
                            )
                    )
                )
            }
        }
    }

    companion object {
        private const val PREFS_NAME =
            "financial_ingestion_diagnostics_v1"
        private const val KEY_ENTRIES = "entries"
        private const val MAX_ENTRIES = 500
        private const val MAX_TITLE_LENGTH = 80
        private const val MAX_SOURCE_TYPE_LENGTH = 24
        private const val MAX_EVENT_LABEL_LENGTH = 48
        private const val MAX_PACKAGE_LENGTH = 120
        private const val MAX_SOURCE_KIND_LENGTH = 80

        @Volatile
        private var instance:
            FinancialIngestionDiagnosticsStore? = null

        fun get(
            context: Context
        ): FinancialIngestionDiagnosticsStore =
            instance ?: synchronized(this) {
                instance
                    ?: FinancialIngestionDiagnosticsStore(
                        context.applicationContext
                    ).also {
                        instance = it
                    }
            }
    }
}

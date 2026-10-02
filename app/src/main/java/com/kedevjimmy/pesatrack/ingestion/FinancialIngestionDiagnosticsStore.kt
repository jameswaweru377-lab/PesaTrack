package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import com.kedevjimmy.pesatrack.domain.messagepack.MessageParseProvenance
import com.kedevjimmy.pesatrack.domain.messagepack.MessageParseRoute
import org.json.JSONArray
import org.json.JSONObject

enum class FinancialIngestionDiagnosticCode {
    UNRECOGNIZED_FINANCIAL_LANGUAGE,
    RESOLVED_BY_CURRENT_COVERAGE,
    UNDERSTOOD_NO_LEDGER_EFFECT,
    LINKED_OBSERVATION,
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
    val pendingReviewEvents: Int,
    val linkedTransactionId: Long?,
    val parseRoute: MessageParseRoute =
        MessageParseRoute.GENERIC_FALLBACK,
    val templateId: String? = null,
    val templateRevision: Int? = null,
    val extractedFieldNames: List<String> =
        emptyList(),
    val messageShapeId: String? = null
)

data class FinancialMessageCoverageSnapshot(
    val totalFinancialObservations: Int,
    val templateParsed: Int,
    val genericFallbackParsed: Int,
    val understoodNoLedgerEffect: Int,
    val unsupportedFinancial: Int,
    val resolvedByCurrentCoverage: Int = 0
) {
    val templateCoveragePercent: Int
        get() =
            if (totalFinancialObservations == 0) {
                0
            } else {
                (
                    templateParsed * 100.0 /
                        totalFinancialObservations
                ).toInt()
            }
}

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
        linkedTransactionId: Long?,
        linkedByCorrelation: Boolean,
        result: FinancialMessageIngestionResult,
        parseProvenance: MessageParseProvenance,
        messageShapeId: String? = null
    ) {
        if (!financialCandidate && !result.recognized) {
            return
        }

        val code = when {
            linkedByCorrelation ->
                FinancialIngestionDiagnosticCode
                    .LINKED_OBSERVATION
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
                    "parseRoute",
                    parseProvenance.route.name
                )
                put(
                    "templateId",
                    parseProvenance.templateId
                        ?: JSONObject.NULL
                )
                put(
                    "templateRevision",
                    parseProvenance
                        .templateRevision
                        ?: JSONObject.NULL
                )
                put(
                    "extractedFieldNames",
                    JSONArray().apply {
                        parseProvenance
                            .extractedFields
                            .map {
                                it.field
                            }
                            .distinct()
                            .sorted()
                            .forEach {
                                fieldName ->
                                put(fieldName)
                            }
                    }
                )
                if (
                    messageShapeId != null
                ) {
                    put(
                        "messageShapeId",
                        messageShapeId
                    )
                } else {
                    put(
                        "messageShapeId",
                        JSONObject.NULL
                    )
                }
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
                if (linkedTransactionId != null) {
                    put(
                        "linkedTransactionId",
                        linkedTransactionId
                    )
                } else {
                    put(
                        "linkedTransactionId",
                        JSONObject.NULL
                    )
                }
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
                            ),
                        linkedTransactionId =
                            if (
                                item.isNull(
                                    "linkedTransactionId"
                                )
                            ) {
                                null
                            } else {
                                item.optLong(
                                    "linkedTransactionId"
                                )
                            },
                        parseRoute =
                            runCatching {
                                MessageParseRoute
                                    .valueOf(
                                        item.optString(
                                            "parseRoute",
                                            MessageParseRoute
                                                .GENERIC_FALLBACK
                                                .name
                                        )
                                    )
                            }.getOrDefault(
                                MessageParseRoute
                                    .GENERIC_FALLBACK
                            ),
                        templateId =
                            if (
                                item.isNull(
                                    "templateId"
                                )
                            ) {
                                null
                            } else {
                                item.optString(
                                    "templateId"
                                ).trim()
                                    .takeIf(
                                        String::isNotBlank
                                    )
                            },
                        templateRevision =
                            if (
                                item.isNull(
                                    "templateRevision"
                                )
                            ) {
                                null
                            } else {
                                item.optInt(
                                    "templateRevision"
                                )
                            },
                        extractedFieldNames =
                            item.optJSONArray(
                                "extractedFieldNames"
                            )?.let {
                                names ->
                                buildList {
                                    for (
                                        nameIndex in
                                            0 until
                                                names.length()
                                    ) {
                                        names.optString(
                                            nameIndex
                                        ).trim()
                                            .takeIf(
                                                String::isNotBlank
                                            )
                                            ?.let(::add)
                                    }
                                }
                            }.orEmpty(),
                        messageShapeId =
                            if (
                                item.isNull(
                                    "messageShapeId"
                                )
                            ) {
                                null
                            } else {
                                item.optString(
                                    "messageShapeId"
                                ).trim()
                                    .takeIf(
                                        String::isNotBlank
                                    )
                            }
                    )
                )
            }
        }
    }

    @Synchronized
    fun markShapeCovered(
        shapeId: String
    ): Int {
        if (shapeId.isBlank()) {
            return 0
        }

        val current =
            runCatching {
                JSONArray(
                    preferences.getString(
                        KEY_ENTRIES,
                        "[]"
                    ) ?: "[]"
                )
            }.getOrElse {
                JSONArray()
            }
        var changed = 0

        for (
            index in 0 until
                current.length()
        ) {
            val item =
                current.getJSONObject(
                    index
                )
            val sameShape =
                item.optString(
                    "messageShapeId"
                ) == shapeId
            val unsupported =
                item.optString(
                    "code"
                ) ==
                    FinancialIngestionDiagnosticCode
                        .UNRECOGNIZED_FINANCIAL_LANGUAGE
                        .name

            if (
                sameShape &&
                unsupported
            ) {
                item.put(
                    "code",
                    FinancialIngestionDiagnosticCode
                        .RESOLVED_BY_CURRENT_COVERAGE
                        .name
                )
                changed += 1
            }
        }

        if (changed > 0) {
            preferences.edit()
                .putString(
                    KEY_ENTRIES,
                    current.toString()
                )
                .apply()
        }

        return changed
    }

    fun sourceCoverage(
        observationLimit: Int = MAX_ENTRIES,
        sourceLimit: Int = 8
    ): List<FinancialSourceCoverageSnapshot> =
        FinancialCoverageAnalysis.bySource(
            entries =
                recent(
                    observationLimit
                ),
            limit =
                sourceLimit
        )

    fun coverageSnapshot(
        limit: Int = MAX_ENTRIES
    ): FinancialMessageCoverageSnapshot {
        val entries =
            recent(limit)
        return FinancialMessageCoverageSnapshot(
            totalFinancialObservations =
                entries.size,
            templateParsed =
                entries.count {
                    it.parseRoute ==
                        MessageParseRoute
                            .TEMPLATE
                },
            genericFallbackParsed =
                entries.count {
                    it.parseRoute ==
                        MessageParseRoute
                            .GENERIC_FALLBACK
                },
            understoodNoLedgerEffect =
                entries.count {
                    it.code ==
                        FinancialIngestionDiagnosticCode
                            .UNDERSTOOD_NO_LEDGER_EFFECT
                },
            resolvedByCurrentCoverage =
                entries.count {
                    it.code ==
                        FinancialIngestionDiagnosticCode
                            .RESOLVED_BY_CURRENT_COVERAGE
                },
            unsupportedFinancial =
                entries.count {
                    it.code ==
                        FinancialIngestionDiagnosticCode
                            .UNRECOGNIZED_FINANCIAL_LANGUAGE
                }
        )
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

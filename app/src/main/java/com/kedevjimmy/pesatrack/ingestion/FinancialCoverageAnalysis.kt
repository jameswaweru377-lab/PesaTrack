package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.messagepack.MessageParseRoute

data class FinancialSourceCoverageSnapshot(
    val sourceKey: String,
    val displayName: String,
    val sourcePackage: String?,
    val sourceKind: String,
    val totalObservations: Int,
    val understoodObservations: Int,
    val templateParsed: Int,
    val unsupportedFinancial: Int,
    val latestObservedAtEpochMs: Long
) {
    val understoodPercent: Int
        get() =
            if (totalObservations == 0) {
                0
            } else {
                (
                    understoodObservations *
                        100.0 /
                        totalObservations
                ).toInt()
            }
}

object FinancialCoverageAnalysis {
    fun bySource(
        entries:
            List<FinancialIngestionDiagnostic>,
        limit: Int = 8
    ): List<FinancialSourceCoverageSnapshot> =
        entries
            .groupBy(::sourceKey)
            .map {
                    (key, grouped) ->
                val latest =
                    grouped.maxBy {
                        it.receivedAtEpochMs
                    }
                val unsupported =
                    grouped.count {
                        it.code ==
                            FinancialIngestionDiagnosticCode
                                .UNRECOGNIZED_FINANCIAL_LANGUAGE
                    }

                FinancialSourceCoverageSnapshot(
                    sourceKey = key,
                    displayName =
                        latest.sourceDisplayName
                            ?.takeIf(
                                String::isNotBlank
                            )
                            ?: latest.title
                                ?.takeIf(
                                    String::isNotBlank
                                )
                            ?: latest.sourcePackage
                                ?.takeIf(
                                    String::isNotBlank
                                )
                            ?: "Unknown financial source",
                    sourcePackage =
                        latest.sourcePackage,
                    sourceKind =
                        latest.sourceKind,
                    totalObservations =
                        grouped.size,
                    understoodObservations =
                        grouped.size -
                            unsupported,
                    templateParsed =
                        grouped.count {
                            it.parseRoute ==
                                MessageParseRoute
                                    .TEMPLATE
                        },
                    unsupportedFinancial =
                        unsupported,
                    latestObservedAtEpochMs =
                        latest.receivedAtEpochMs
                )
            }
            .sortedWith(
                compareByDescending<
                    FinancialSourceCoverageSnapshot
                > {
                    it.unsupportedFinancial
                }.thenByDescending {
                    it.totalObservations
                }.thenByDescending {
                    it.latestObservedAtEpochMs
                }
            )
            .take(
                limit.coerceIn(
                    1,
                    50
                )
            )

    private fun sourceKey(
        entry:
            FinancialIngestionDiagnostic
    ): String {
        val packagePart =
            entry.sourcePackage
                ?.trim()
                ?.lowercase()
                ?.takeIf(
                    String::isNotBlank
                )
        val displayPart =
            (
                entry.sourceDisplayName
                    ?: entry.title
                    ?: "unknown"
            ).trim()
                .lowercase()
                .replace(
                    Regex(
                        """[^a-z0-9]+"""
                    ),
                    "-"
                )
                .trim('-')
                .ifBlank {
                    "unknown"
                }
        val kind =
            entry.sourceKind
                .trim()
                .lowercase()
                .ifBlank {
                    "financial"
                }

        return kind +
            "|" +
            (
                packagePart
                    ?: displayPart
            )
    }
}

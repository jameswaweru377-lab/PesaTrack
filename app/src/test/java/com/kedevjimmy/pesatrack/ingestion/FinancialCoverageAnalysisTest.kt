package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.messagepack.MessageParseRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialCoverageAnalysisTest {
    @Test
    fun groupsAppNotificationsByPackageAndChannel() {
        val entries =
            listOf(
                diagnostic(
                    name = "Equity Bank Kenya",
                    packageName =
                        "ke.co.equitygroup.equitymobile",
                    sourceKind =
                        "financial_notification",
                    code =
                        FinancialIngestionDiagnosticCode
                            .INGESTED,
                    route =
                        MessageParseRoute.TEMPLATE,
                    time = 100L
                ),
                diagnostic(
                    name = "Equity Mobile",
                    packageName =
                        "KE.CO.EQUITYGROUP.EQUITYMOBILE",
                    sourceKind =
                        "financial_notification",
                    code =
                        FinancialIngestionDiagnosticCode
                            .UNRECOGNIZED_FINANCIAL_LANGUAGE,
                    route =
                        MessageParseRoute
                            .GENERIC_FALLBACK,
                    time = 200L
                )
            )

        val source =
            FinancialCoverageAnalysis
                .bySource(entries)
                .single()

        assertEquals(
            2,
            source.totalObservations
        )
        assertEquals(
            1,
            source.understoodObservations
        )
        assertEquals(
            1,
            source.templateParsed
        )
        assertEquals(
            1,
            source.unsupportedFinancial
        )
        assertEquals(
            50,
            source.understoodPercent
        )
        assertEquals(
            "Equity Mobile",
            source.displayName
        )
    }

    @Test
    fun keepsSmsAndAppCoverageSeparate() {
        val entries =
            listOf(
                diagnostic(
                    name = "KCB Bank Kenya",
                    packageName = null,
                    sourceKind =
                        "historical_financial_message",
                    code =
                        FinancialIngestionDiagnosticCode
                            .INGESTED,
                    route =
                        MessageParseRoute.TEMPLATE,
                    time = 100L
                ),
                diagnostic(
                    name = "KCB Bank Kenya",
                    packageName =
                        "com.kcb.mobilebanking.android.mbp",
                    sourceKind =
                        "financial_notification",
                    code =
                        FinancialIngestionDiagnosticCode
                            .INGESTED,
                    route =
                        MessageParseRoute.TEMPLATE,
                    time = 200L
                )
            )

        val sources =
            FinancialCoverageAnalysis
                .bySource(entries)

        assertEquals(
            2,
            sources.size
        )
    }

    @Test
    fun sourceKeyNormalizesPackageAndChannelForMessageLabDrillDown() {
        assertEquals(
            "financial_notification|ke.co.equitygroup.equitymobile",
            FinancialCoverageAnalysis
                .sourceKey(
                    sourceKind =
                        " Financial_Notification ",
                    sourcePackage =
                        "KE.CO.EQUITYGROUP.EQUITYMOBILE",
                    displayName =
                        "Equity Mobile"
                )
        )
    }

    @Test
    fun sourceKeyFallsBackToStableDisplayIdentityWithoutPackage() {
        assertEquals(
            "historical_financial_message|mwalimu-national-sacco",
            FinancialCoverageAnalysis
                .sourceKey(
                    sourceKind =
                        "historical_financial_message",
                    sourcePackage = null,
                    displayName =
                        "Mwalimu National Sacco"
                )
        )
    }

    @Test
    fun unsupportedSourcesSortFirstToExposeCoverageGaps() {
        val understood =
            diagnostic(
                name = "Known Bank",
                packageName = null,
                sourceKind =
                    "historical_financial_message",
                code =
                    FinancialIngestionDiagnosticCode
                        .INGESTED,
                route =
                    MessageParseRoute.TEMPLATE,
                time = 200L
            )
        val unsupported =
            diagnostic(
                name = "Future Sacco",
                packageName = null,
                sourceKind =
                    "historical_financial_message",
                code =
                    FinancialIngestionDiagnosticCode
                        .UNRECOGNIZED_FINANCIAL_LANGUAGE,
                route =
                    MessageParseRoute
                        .GENERIC_FALLBACK,
                time = 100L
            )

        val sources =
            FinancialCoverageAnalysis
                .bySource(
                    listOf(
                        understood,
                        unsupported
                    )
                )

        assertEquals(
            "Future Sacco",
            sources.first().displayName
        )
    }

    private fun diagnostic(
        name: String,
        packageName: String?,
        sourceKind: String,
        code:
            FinancialIngestionDiagnosticCode,
        route: MessageParseRoute,
        time: Long
    ) =
        FinancialIngestionDiagnostic(
            title = name,
            sourceDisplayName = name,
            sourceType = "Bank",
            eventLabel =
                "Financial message",
            sourcePackage =
                packageName,
            sourceKind =
                sourceKind,
            receivedAtEpochMs = time,
            code = code,
            insertedTransactions =
                if (
                    code ==
                    FinancialIngestionDiagnosticCode
                        .INGESTED
                ) {
                    1
                } else {
                    0
                },
            insertedFinancialEvents = 0,
            pendingReviewEvents = 0,
            linkedTransactionId = null,
            parseRoute = route
        )
}

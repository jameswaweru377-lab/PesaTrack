package com.kedevjimmy.pesatrack.ingestion

enum class ResolvedCoverageCheckState(
    val displayLabel: String
) {
    STILL_COVERED(
        "Still covered"
    ),
    COVERED_DIFFERENT_ROUTE(
        "Covered by a different route"
    ),
    STILL_RETIRED(
        "Still retired"
    ),
    POSSIBLE_REGRESSION(
        "Possible regression"
    ),
    INCONCLUSIVE(
        "Inconclusive"
    )
}

data class ResolvedCoverageCheck(
    val shapeId: String,
    val state:
        ResolvedCoverageCheckState,
    val archivedRoute: String?,
    val archivedTemplateId: String?,
    val currentRoute: String?,
    val currentTemplateId: String?,
    val currentRecognized: Boolean,
    val currentSemanticallyUnderstood:
        Boolean,
    val currentWouldEnterMessageLab:
        Boolean,
    val explanation: String
)

object MessageLabResolutionRegressionChecker {
    fun check(
        record:
            ResolvedMessageLabShape
    ): ResolvedCoverageCheck {
        val replayBody =
            syntheticReplayBody(
                record.redactedBody
            )
        val title =
            record.sender
                .takeIf {
                    it.isNotBlank() &&
                        it != "<SENDER>"
                }
        val inspection =
            FinancialMessageInspector
                .inspect(
                    title = title,
                    body = replayBody
                )

        return classify(
            record = record,
            inspection = inspection
        )
    }

    fun checkAll(
        records:
            List<ResolvedMessageLabShape>
    ): List<ResolvedCoverageCheck> =
        records.map(::check)

    fun classify(
        record:
            ResolvedMessageLabShape,
        inspection:
            FinancialMessageInspection
    ): ResolvedCoverageCheck {
        val currentlyCovered =
            inspection.recognized ||
                inspection
                    .semanticallyUnderstood

        val state =
            when (
                record.resolutionKind
            ) {
                MessageLabResolutionKind
                    .PARSER_COVERAGE ->
                    when {
                        currentlyCovered &&
                            sameRoute(
                                record =
                                    record,
                                inspection =
                                    inspection
                            ) ->
                            ResolvedCoverageCheckState
                                .STILL_COVERED

                        currentlyCovered ->
                            ResolvedCoverageCheckState
                                .COVERED_DIFFERENT_ROUTE

                        inspection
                            .wouldEnterMessageLab ->
                            ResolvedCoverageCheckState
                                .POSSIBLE_REGRESSION

                        else ->
                            ResolvedCoverageCheckState
                                .INCONCLUSIVE
                    }

                MessageLabResolutionKind
                    .NO_LONGER_UNSUPPORTED ->
                    if (
                        inspection
                            .wouldEnterMessageLab
                    ) {
                        ResolvedCoverageCheckState
                            .POSSIBLE_REGRESSION
                    } else {
                        ResolvedCoverageCheckState
                            .STILL_RETIRED
                    }
            }

        return ResolvedCoverageCheck(
            shapeId =
                record.shapeId,
            state =
                state,
            archivedRoute =
                record.parseRoute,
            archivedTemplateId =
                record.templateId,
            currentRoute =
                inspection
                    .parseRoute
                    .name,
            currentTemplateId =
                inspection.templateId,
            currentRecognized =
                inspection.recognized,
            currentSemanticallyUnderstood =
                inspection
                    .semanticallyUnderstood,
            currentWouldEnterMessageLab =
                inspection
                    .wouldEnterMessageLab,
            explanation =
                explanationFor(
                    record = record,
                    inspection =
                        inspection,
                    state = state
                )
        )
    }

    fun syntheticReplayBody(
        redactedBody: String
    ): String {
        var replay =
            redactedBody

        val replacements =
            linkedMapOf(
                "<DATE_TIME>" to
                    "2/10/26 at 7:33 AM",
                "<TX_CODE>" to
                    "TSTREG0001",
                "<AMOUNT>" to
                    "123.45",
                "<DATE>" to
                    "2/10/26",
                "<TIME>" to
                    "7:33 AM",
                "<PHONE>" to
                    "0712345678",
                "<EMAIL>" to
                    "test@example.com",
                "<ACCOUNT>" to
                    "XX1234",
                "<REFERENCE>" to
                    "ABC12345",
                "<LIABILITY>" to
                    "Sample Loan",
                "<ENTITY>" to
                    "SAMPLE STORE",
                "<PERSON>" to
                    "SAMPLE USER",
                "<NUMBER>" to
                    "123456",
                "<SOURCE>" to
                    "M-PESA",
                "<DESTINATION>" to
                    "SAMPLE ACCOUNT",
                "<COUNTERPARTY>" to
                    "SAMPLE STORE",
                "<VALUE>" to
                    "SAMPLE"
            )

        replacements.forEach {
            (placeholder, value) ->
            replay =
                replay.replace(
                    placeholder,
                    value
                )
        }

        return replay.replace(
            Regex(
                """<[A-Z_]+>"""
            ),
            "SAMPLE"
        )
    }

    private fun sameRoute(
        record:
            ResolvedMessageLabShape,
        inspection:
            FinancialMessageInspection
    ): Boolean {
        val routeMatches =
            record.parseRoute == null ||
                record.parseRoute ==
                inspection
                    .parseRoute
                    .name
        val templateMatches =
            record.templateId == null ||
                record.templateId ==
                inspection.templateId

        return routeMatches &&
            templateMatches
    }

    private fun explanationFor(
        record:
            ResolvedMessageLabShape,
        inspection:
            FinancialMessageInspection,
        state:
            ResolvedCoverageCheckState
    ): String =
        when (state) {
            ResolvedCoverageCheckState
                .STILL_COVERED ->
                "A safe synthetic replay of the archived redacted shape is still understood through the recorded parser route."

            ResolvedCoverageCheckState
                .COVERED_DIFFERENT_ROUTE ->
                "The archived shape is still understood, but the current parser reaches it through a different route or template."

            ResolvedCoverageCheckState
                .STILL_RETIRED ->
                "A safe synthetic replay still does not qualify as unsupported financial evidence."

            ResolvedCoverageCheckState
                .POSSIBLE_REGRESSION ->
                if (
                    record.resolutionKind ==
                    MessageLabResolutionKind
                        .PARSER_COVERAGE
                ) {
                    "A safe synthetic replay is no longer understood and would return to Message Lab. Re-check this shape with a real sanitized example before changing parser code."
                } else {
                    "A shape previously retired by relevance would now enter Message Lab again. Review the relevance policy before treating this as a parser failure."
                }

            ResolvedCoverageCheckState
                .INCONCLUSIVE ->
                if (
                    inspection.recognized ||
                    inspection
                        .semanticallyUnderstood
                ) {
                    "The current build still understands the shape, but the recorded route cannot be compared reliably."
                } else {
                    "The synthetic replay is not understood, but it also would not enter Message Lab. The sanitized archive is insufficient to call this a regression."
                }
        }
}

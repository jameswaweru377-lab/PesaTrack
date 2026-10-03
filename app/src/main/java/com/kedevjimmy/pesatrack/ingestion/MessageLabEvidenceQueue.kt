package com.kedevjimmy.pesatrack.ingestion

object MessageLabEvidenceQueue {
    fun prioritize(
        shapes:
            List<UnsupportedMessageShape>
    ): List<UnsupportedMessageShape> =
        shapes.sortedWith(
            compareByDescending<
                UnsupportedMessageShape
            > {
                it.userDecision ==
                    MessageLabUserDecision
                        .CONFIRMED_FINANCIAL
            }.thenByDescending {
                it.semanticLabel != null
            }.thenByDescending {
                when (
                    it.implementationReview
                ) {
                    MessageLabImplementationReviewState
                        .READY_FOR_PARSER_WORK ->
                        2

                    null ->
                        1

                    MessageLabImplementationReviewState
                        .NEEDS_MORE_EVIDENCE ->
                        0
                }
            }.thenByDescending {
                it.occurrenceCount
            }.thenByDescending {
                it.lastSeenAtEpochMs
            }.thenBy {
                it.shapeId
            }
        )

    fun confirmedFinancial(
        shapes:
            List<UnsupportedMessageShape>
    ): List<UnsupportedMessageShape> =
        prioritize(
            shapes.filter {
                it.userDecision ==
                    MessageLabUserDecision
                        .CONFIRMED_FINANCIAL
            }
        )
}

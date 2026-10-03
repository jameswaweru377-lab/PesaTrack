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
                maxOf(
                    it.occurrenceCount,
                    it.familyOccurrenceCount
                )
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

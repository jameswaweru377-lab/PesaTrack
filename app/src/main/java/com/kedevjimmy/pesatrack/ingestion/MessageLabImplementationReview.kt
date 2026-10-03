package com.kedevjimmy.pesatrack.ingestion

enum class MessageLabImplementationReviewState(
    val displayLabel: String
) {
    NEEDS_MORE_EVIDENCE(
        "Needs more examples"
    ),
    READY_FOR_PARSER_WORK(
        "Ready for parser work"
    )
}

data class MessageLabImplementationReviewSnapshot(
    val evidenceReadyCount: Int,
    val unreviewedCount: Int,
    val needsMoreEvidenceCount: Int,
    val readyForParserWorkCount: Int
)

object MessageLabImplementationReview {
    fun isReviewable(
        shape: UnsupportedMessageShape
    ): Boolean =
        MessageLabParserReadiness
            .stateFor(shape) ==
            MessageLabParserReadinessState
                .EVIDENCE_READY

    fun snapshot(
        shapes:
            List<UnsupportedMessageShape>
    ): MessageLabImplementationReviewSnapshot {
        val reviewable =
            MessageLabParserReadiness
                .evidenceReady(
                    shapes
                )

        return MessageLabImplementationReviewSnapshot(
            evidenceReadyCount =
                reviewable.size,
            unreviewedCount =
                reviewable.count {
                    it.implementationReview ==
                        null
                },
            needsMoreEvidenceCount =
                reviewable.count {
                    it.implementationReview ==
                        MessageLabImplementationReviewState
                            .NEEDS_MORE_EVIDENCE
                },
            readyForParserWorkCount =
                reviewable.count {
                    it.implementationReview ==
                        MessageLabImplementationReviewState
                            .READY_FOR_PARSER_WORK
                }
        )
    }

    fun readyForParserWork(
        shapes:
            List<UnsupportedMessageShape>
    ): List<UnsupportedMessageShape> =
        MessageLabParserReadiness
            .evidenceReady(
                shapes
            )
            .filter {
                it.implementationReview ==
                    MessageLabImplementationReviewState
                        .READY_FOR_PARSER_WORK
            }
}

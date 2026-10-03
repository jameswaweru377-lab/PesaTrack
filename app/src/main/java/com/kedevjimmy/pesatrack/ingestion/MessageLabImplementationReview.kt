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
        val groups =
            reviewGroups(shapes)

        return MessageLabImplementationReviewSnapshot(
            evidenceReadyCount =
                groups.size,
            unreviewedCount =
                groups.count {
                    (_, family) ->
                    family.none {
                        it.implementationReview !=
                            null
                    }
                },
            needsMoreEvidenceCount =
                groups.count {
                    (_, family) ->
                    family.none {
                        it.implementationReview ==
                            MessageLabImplementationReviewState
                                .READY_FOR_PARSER_WORK
                    } &&
                        family.any {
                            it.implementationReview ==
                                MessageLabImplementationReviewState
                                    .NEEDS_MORE_EVIDENCE
                        }
                },
            readyForParserWorkCount =
                groups.count {
                    (_, family) ->
                    family.any {
                        it.implementationReview ==
                            MessageLabImplementationReviewState
                                .READY_FOR_PARSER_WORK
                    }
                }
        )
    }

    fun readyForParserWork(
        shapes:
            List<UnsupportedMessageShape>
    ): List<UnsupportedMessageShape> =
        reviewGroups(shapes)
            .values
            .mapNotNull {
                family ->
                MessageLabEvidenceQueue
                    .prioritize(
                        family.filter {
                            it.implementationReview ==
                                MessageLabImplementationReviewState
                                    .READY_FOR_PARSER_WORK
                        }
                    )
                    .firstOrNull()
            }
            .let(
                MessageLabEvidenceQueue::prioritize
            )

    private fun reviewGroups(
        shapes:
            List<UnsupportedMessageShape>
    ): Map<String, List<UnsupportedMessageShape>> =
        shapes
            .filter(
                ::isReviewable
            )
            .groupBy {
                reviewKey(it)
            }

    private fun reviewKey(
        shape: UnsupportedMessageShape
    ): String =
        shape.familyId +
            "|" +
            (
                shape.semanticLabel
                    ?.name
                    ?: "UNLABELED"
                )
}

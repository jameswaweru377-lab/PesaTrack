package com.kedevjimmy.pesatrack.ingestion

enum class MessageLabParserReadinessState {
    NEEDS_FINANCIAL_CONFIRMATION,
    NEEDS_MEANING_LABEL,
    LABELED_SINGLE_OBSERVATION,
    EVIDENCE_READY
}

data class MessageLabParserReadinessSnapshot(
    val confirmedFinancialCount: Int,
    val meaningLabeledCount: Int,
    val evidenceReadyCount: Int,
    val needsMeaningCount: Int
)

object MessageLabParserReadiness {
    const val MIN_REPEATED_OCCURRENCES = 2

    fun stateFor(
        shape: UnsupportedMessageShape
    ): MessageLabParserReadinessState =
        when {
            shape.userDecision !=
                MessageLabUserDecision
                    .CONFIRMED_FINANCIAL ->
                MessageLabParserReadinessState
                    .NEEDS_FINANCIAL_CONFIRMATION

            shape.semanticLabel == null ->
                MessageLabParserReadinessState
                    .NEEDS_MEANING_LABEL

            shape.occurrenceCount <
                MIN_REPEATED_OCCURRENCES ->
                MessageLabParserReadinessState
                    .LABELED_SINGLE_OBSERVATION

            else ->
                MessageLabParserReadinessState
                    .EVIDENCE_READY
        }

    fun snapshot(
        shapes:
            List<UnsupportedMessageShape>
    ): MessageLabParserReadinessSnapshot {
        val confirmed =
            shapes.filter {
                it.userDecision ==
                    MessageLabUserDecision
                        .CONFIRMED_FINANCIAL
            }
        val labeled =
            confirmed.filter {
                it.semanticLabel != null
            }

        return MessageLabParserReadinessSnapshot(
            confirmedFinancialCount =
                confirmed.size,
            meaningLabeledCount =
                labeled.size,
            evidenceReadyCount =
                labeled.count {
                    stateFor(it) ==
                        MessageLabParserReadinessState
                            .EVIDENCE_READY
                },
            needsMeaningCount =
                confirmed.count {
                    it.semanticLabel == null
                }
        )
    }

    fun evidenceReady(
        shapes:
            List<UnsupportedMessageShape>
    ): List<UnsupportedMessageShape> =
        MessageLabEvidenceQueue
            .prioritize(
                shapes.filter {
                    stateFor(it) ==
                        MessageLabParserReadinessState
                            .EVIDENCE_READY
                }
            )
}

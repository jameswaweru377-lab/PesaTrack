package com.kedevjimmy.pesatrack.ingestion

data class MessageInspectorParserHandoffAvailability(
    val parserDraftAvailable: Boolean,
    val implementationPacketAvailable:
        Boolean
)

object MessageInspectorParserHandoffPolicy {
    fun availabilityFor(
        shape: UnsupportedMessageShape?
    ): MessageInspectorParserHandoffAvailability {
        if (shape == null) {
            return MessageInspectorParserHandoffAvailability(
                parserDraftAvailable =
                    false,
                implementationPacketAvailable =
                    false
            )
        }

        val evidenceReady =
            MessageLabParserReadiness
                .stateFor(shape) ==
                MessageLabParserReadinessState
                    .EVIDENCE_READY

        return MessageInspectorParserHandoffAvailability(
            parserDraftAvailable =
                evidenceReady,
            implementationPacketAvailable =
                evidenceReady &&
                    shape.implementationReview ==
                    MessageLabImplementationReviewState
                        .READY_FOR_PARSER_WORK
        )
    }
}

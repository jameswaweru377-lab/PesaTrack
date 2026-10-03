package com.kedevjimmy.pesatrack.ingestion

enum class MessageLabUserDecision {
    CONFIRMED_FINANCIAL,
    NOT_FINANCIAL
}

object MessageLabDecisionPolicy {
    fun retainUnsupported(
        heuristicUnsupported: Boolean,
        userDecision:
            MessageLabUserDecision?
    ): Boolean =
        when (userDecision) {
            MessageLabUserDecision
                .CONFIRMED_FINANCIAL ->
                true

            MessageLabUserDecision
                .NOT_FINANCIAL ->
                false

            null ->
                heuristicUnsupported
        }

    fun canRetireAsUnderstood(
        recognized: Boolean,
        semanticallyUnderstood: Boolean,
        userDecision:
            MessageLabUserDecision?
    ): Boolean =
        userDecision !=
            MessageLabUserDecision
                .NOT_FINANCIAL &&
            (
                recognized ||
                    semanticallyUnderstood
            )
}

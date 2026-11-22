package com.kedevjimmy.pesatrack.security

enum class PinEntryAction { WAIT, VERIFY, VERIFY_LEGACY_START, VERIFY_LEGACY_CONTINUATION }

/** The first successful unlock of an older PIN persists its length for later entries. */
object PinAutoSubmitPolicy {
    /** A delayed PIN check may only complete in the foreground session that started it. */
    fun mayCompleteAfterDelay(startEpoch: Int, currentEpoch: Int, foreground: Boolean): Boolean =
        foreground && startEpoch == currentEpoch

    fun onChange(pin: String, storedLength: Int?, isDeletion: Boolean = false): PinEntryAction {
        // Backspacing an older PIN is a correction, not a new failed unlock attempt.
        // The previous verification is cancelled by Compose when the PIN changes.
        if (isDeletion) return PinEntryAction.WAIT
        if (pin.any { !it.isDigit() } || pin.length > 8) return PinEntryAction.WAIT
        if (storedLength != null && storedLength in 4..8) {
            return if (pin.length == storedLength) PinEntryAction.VERIFY else PinEntryAction.WAIT
        }
        return when (pin.length) {
            4 -> PinEntryAction.VERIFY_LEGACY_START
            in 5..8 -> PinEntryAction.VERIFY_LEGACY_CONTINUATION
            else -> PinEntryAction.WAIT
        }
    }

    fun shouldRecordFailure(action: PinEntryAction): Boolean =
        action == PinEntryAction.VERIFY || action == PinEntryAction.VERIFY_LEGACY_START

    fun isSameLegacyEntry(previous: String?, candidate: String): Boolean =
        previous != null && previous.length in 4..7 &&
            candidate.length in (previous.length + 1)..8 && candidate.startsWith(previous)
}

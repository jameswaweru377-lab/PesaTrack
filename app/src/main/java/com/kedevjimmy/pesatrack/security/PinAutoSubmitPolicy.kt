package com.kedevjimmy.pesatrack.security

enum class PinEntryAction { WAIT, VERIFY }

/** Unknown legacy PIN lengths require one manual keyboard Done to preserve lockout limits. */
object PinAutoSubmitPolicy {
    fun onChange(pin: String, storedLength: Int?): PinEntryAction {
        if (pin.any { !it.isDigit() } || pin.length > 8) return PinEntryAction.WAIT
        if (storedLength in 4..8) {
            return if (pin.length == storedLength) PinEntryAction.VERIFY else PinEntryAction.WAIT
        }
        return if (pin.length == 8) PinEntryAction.VERIFY else PinEntryAction.WAIT
    }

    fun onDone(pin: String): PinEntryAction =
        if (pin.length in 4..8 && pin.all(Char::isDigit)) PinEntryAction.VERIFY
        else PinEntryAction.WAIT
}

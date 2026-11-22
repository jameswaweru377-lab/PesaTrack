package com.kedevjimmy.pesatrack.security

import org.junit.Assert.assertEquals
import org.junit.Test

class PinAutoSubmitPolicyTest {
    @Test fun knownLengthSubmitsOnlyWhenAllDigitsAreEntered() {
        assertEquals(PinEntryAction.WAIT, PinAutoSubmitPolicy.onChange("12345", 6))
        assertEquals(PinEntryAction.VERIFY, PinAutoSubmitPolicy.onChange("123456", 6))
        assertEquals(PinEntryAction.WAIT, PinAutoSubmitPolicy.onChange("12ab", 6))
    }

    @Test fun legacyLengthDoesNotGuessPartialPinLengths() {
        assertEquals(PinEntryAction.WAIT, PinAutoSubmitPolicy.onChange("1234", null))
        assertEquals(PinEntryAction.WAIT, PinAutoSubmitPolicy.onChange("123456", null))
        assertEquals(PinEntryAction.WAIT, PinAutoSubmitPolicy.onChange("1234567", null))
        assertEquals(PinEntryAction.VERIFY, PinAutoSubmitPolicy.onChange("12345678", null))
    }

    @Test fun keyboardDoneCanVerifyAnOldPinWithoutStoredLength() {
        assertEquals(PinEntryAction.VERIFY, PinAutoSubmitPolicy.onDone("1234"))
        assertEquals(PinEntryAction.VERIFY, PinAutoSubmitPolicy.onDone("123456"))
        assertEquals(PinEntryAction.WAIT, PinAutoSubmitPolicy.onDone("123"))
    }
}

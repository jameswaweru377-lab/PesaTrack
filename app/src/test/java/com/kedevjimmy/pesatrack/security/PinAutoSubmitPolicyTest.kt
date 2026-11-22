package com.kedevjimmy.pesatrack.security

import org.junit.Assert.assertEquals
import org.junit.Test

class PinAutoSubmitPolicyTest {
    @Test fun knownLengthSubmitsOnlyWhenAllDigitsAreEntered() {
        assertEquals(PinEntryAction.WAIT, PinAutoSubmitPolicy.onChange("12345", 6))
        assertEquals(PinEntryAction.VERIFY, PinAutoSubmitPolicy.onChange("123456", 6))
        assertEquals(PinEntryAction.WAIT, PinAutoSubmitPolicy.onChange("12ab", 6))
    }

    @Test fun legacyPinAttemptsEveryEligibleLengthWithoutAnySubmitButton() {
        assertEquals(PinEntryAction.WAIT, PinAutoSubmitPolicy.onChange("123", null))
        assertEquals(PinEntryAction.VERIFY_LEGACY_START, PinAutoSubmitPolicy.onChange("1234", null))
        assertEquals(PinEntryAction.VERIFY_LEGACY_CONTINUATION, PinAutoSubmitPolicy.onChange("12345", null))
        assertEquals(PinEntryAction.VERIFY_LEGACY_CONTINUATION, PinAutoSubmitPolicy.onChange("123456", null))
        assertEquals(PinEntryAction.VERIFY_LEGACY_CONTINUATION, PinAutoSubmitPolicy.onChange("1234567", null))
        assertEquals(PinEntryAction.VERIFY_LEGACY_CONTINUATION, PinAutoSubmitPolicy.onChange("12345678", null))
    }

    @Test fun legacyContinuationDoesNotMultiplyFailedAttemptsForOnePinEntry() {
        assertEquals(true, PinAutoSubmitPolicy.shouldRecordFailure(PinEntryAction.VERIFY_LEGACY_START))
        assertEquals(false, PinAutoSubmitPolicy.shouldRecordFailure(PinEntryAction.VERIFY_LEGACY_CONTINUATION))
        assertEquals(true, PinAutoSubmitPolicy.shouldRecordFailure(PinEntryAction.VERIFY))
        assertEquals(true, PinAutoSubmitPolicy.isSameLegacyEntry("1234", "12345"))
        assertEquals(false, PinAutoSubmitPolicy.isSameLegacyEntry("1234", "22345"))
        assertEquals(false, PinAutoSubmitPolicy.isSameLegacyEntry("1234", "123456"))
        assertEquals(false, PinAutoSubmitPolicy.isSameLegacyEntry("12345", "1234"))
    }

    @Test fun invalidPinInputCannotTriggerVerification() {
        assertEquals(PinEntryAction.WAIT, PinAutoSubmitPolicy.onChange("123456789", null))
        assertEquals(PinEntryAction.WAIT, PinAutoSubmitPolicy.onChange("123x", null))
        assertEquals(PinEntryAction.WAIT, PinAutoSubmitPolicy.onChange("1234567", 8))
    }
}

package com.kedevjimmy.pesatrack.security

import org.junit.Assert.assertEquals
import org.junit.Test

class LockKeypadEntryPolicyTest {
    @Test fun knownPinLengthStopsKeypadAtTheExactDigitCount() {
        assertEquals("314159", LockKeypadEntryPolicy.appendDigit("31415", '9', 6))
        assertEquals("314159", LockKeypadEntryPolicy.appendDigit("314159", '2', 6))
        assertEquals(6, LockKeypadEntryPolicy.indicatorCount(6))
    }

    @Test fun olderPinWithoutLengthSupportsFourThroughEightDigits() {
        assertEquals("12345678", LockKeypadEntryPolicy.appendDigit("1234567", '8', null))
        assertEquals("12345678", LockKeypadEntryPolicy.appendDigit("12345678", '9', null))
        assertEquals(8, LockKeypadEntryPolicy.indicatorCount(null))
        assertEquals(4, LockKeypadEntryPolicy.indicatorCount(4))
    }

    @Test fun onlyAsciiDigitsCanEnterThePin() {
        assertEquals("12", LockKeypadEntryPolicy.appendDigit("12", 'a', 6))
        assertEquals("12", LockKeypadEntryPolicy.appendDigit("12", '٣', 6))
    }

    @Test fun alternateKeyboardRetainsUnicodeNumeralsAcceptedDuringPinCreation() {
        assertEquals("١٢٣٤٥٦", LockKeypadEntryPolicy.fromKeyboard("x١٢٣٤٥٦y", 6))
        assertEquals("١٢٣٤٥٦", LockKeypadEntryPolicy.fromKeyboard("١٢٣٤٥٦٧", 6))
        assertEquals("1٢3٤", LockKeypadEntryPolicy.fromKeyboard("a1٢3٤!", null))
    }

    @Test fun backspaceRemovesOneDigitAndCanClearAnError() {
        assertEquals("123", LockKeypadEntryPolicy.removeLast("1234"))
        assertEquals("", LockKeypadEntryPolicy.removeLast(""))
    }
}

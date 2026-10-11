package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.*
import org.junit.Test

class ReceivedSalaryEntryPolicyTest {
    @Test fun receivedManualMoneyNeedsNoOtherEvidence() {
        ReceivedSalaryEntryPolicy.validate(5000000, 1000, 2000, true)
    }
    @Test fun futureDatesAndInvalidAmountsNeverAuthorizeSalary() {
        for ((amount, date) in listOf(0L to 1000L, -1L to 1000L, 100L to 0L, 100L to 2001L)) {
            assertThrows(IllegalArgumentException::class.java) { ReceivedSalaryEntryPolicy.validate(amount, date, 2000, true) }
        }
    }
    @Test fun missingOrDisabledReceivingAccountCannotPost() {
        assertThrows(IllegalArgumentException::class.java) { ReceivedSalaryEntryPolicy.validate(100, 1000, 2000, false) }
    }
}

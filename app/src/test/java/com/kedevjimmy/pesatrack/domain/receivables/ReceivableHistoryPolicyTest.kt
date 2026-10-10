package com.kedevjimmy.pesatrack.domain.receivables

import org.junit.Assert.*
import org.junit.Test

class ReceivableHistoryPolicyTest {
    @Test fun equivalentKenyanPhonesMatchAcrossAccounts() {
        val result = ReceivableHistoryPolicy.match("Norman Mwangi", "0712345678", "N MWANGI", "+254712345678")
        assertEquals(HistoryMatchKind.PHONE, result.kind)
        assertTrue(result.bulkEligible)
    }
    @Test fun sameNameWithConflictingPhoneNeedsIndividualReview() {
        val result = ReceivableHistoryPolicy.match("Norman Mwangi", "0712345678", "Norman Mwangi", "0798765432")
        assertEquals(HistoryMatchKind.CONFLICT, result.kind)
        assertFalse(result.bulkEligible)
    }
    @Test fun exactNamesAreSuggestionsAndPartialNamesAreNotIdentity() {
        assertEquals(HistoryMatchKind.NAME, ReceivableHistoryPolicy.match(" Norman  Mwangi ", null, "NORMAN MWANGI", null).kind)
        assertEquals(HistoryMatchKind.PARTIAL, ReceivableHistoryPolicy.match("Norman", null, "Norman Mwangi", null).kind)
        assertFalse(ReceivableHistoryPolicy.match("Norman", null, "Norman Mwangi", null).bulkEligible)
        assertEquals(HistoryMatchKind.NONE, ReceivableHistoryPolicy.match("", null, null, null).kind)
    }
    @Test fun malformedPhonesNeverBecomeStrongMatches() {
        assertNotEquals(HistoryMatchKind.PHONE, ReceivableHistoryPolicy.match("Norman", "1234567", "Other", "1234567").kind)
    }
}

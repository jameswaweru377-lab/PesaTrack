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
    @Test fun sameNameDifferentPhonesCannotBeSelectedTogetherWithoutConfirmedPhone() {
        val first = ReceivableHistoryPolicy.match("Norman Mwangi",null,"Norman Mwangi","0712345678")
        val second = ReceivableHistoryPolicy.match("Norman Mwangi",null,"Norman Mwangi","0798765432")
        assertFalse(first.bulkEligible)
        assertFalse(second.bulkEligible)
        assertFalse(ReceivableHistoryPolicy.match("Norman Mwangi",null,"Norman Mwangi",null).bulkEligible)
    }
    @Test fun malformedPhonesNeverBecomeStrongMatches() {
        assertNotEquals(HistoryMatchKind.PHONE, ReceivableHistoryPolicy.match("Norman", "1234567", "Other", "1234567").kind)
    }
    @Test fun archivedPhoneCanBeRecoveredFromFormattedStatementDescription() {
        assertEquals("254712345678", ReceivableHistoryPolicy.phoneInStatementDescription(
            "Send Money to Norman Mwangi 0712 345678"))
        assertEquals("254712345678", ReceivableHistoryPolicy.phoneInStatementDescription(
            "Funds from Norman Mwangi +254 712 345 678"))
        assertNull(ReceivableHistoryPolicy.phoneInStatementDescription("PAYBILL 123456789 REF123"))
    }

    @Test fun freeTextPhoneCannotAuthorizeBulkLoanSelection() {
        val inferred = ReceivableHistoryPolicy.matchStatementDescription(
            "Norman Mwangi", "0712345678", "Send Money to Norman Mwangi +254712345678")
        assertEquals(HistoryMatchKind.PHONE, inferred.kind)
        assertFalse(inferred.bulkEligible)
        val conflict = ReceivableHistoryPolicy.matchStatementDescription(
            "Norman Mwangi", "0712345678", "Send Money to Norman Mwangi 0798765432")
        assertEquals(HistoryMatchKind.CONFLICT, conflict.kind)
        assertFalse(conflict.bulkEligible)
    }

    @Test fun ambiguousFreeTextPhoneNumbersDoNotCreateVerifiedIdentity() {
        val sample = "Transfer 0712345678 from 0798765432"
        assertNull(ReceivableHistoryPolicy.phoneInStatementDescription(sample))
        val unverified = ReceivableHistoryPolicy.matchStatementDescription(
            "Norman", "0712345678", sample)
        assertFalse(unverified.bulkEligible)
    }

}

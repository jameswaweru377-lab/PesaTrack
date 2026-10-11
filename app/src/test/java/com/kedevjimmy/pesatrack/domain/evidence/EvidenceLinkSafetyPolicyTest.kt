package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceLinkSafetyPolicyTest {
    private val statement=EvidenceCashFact("account-mpesa","INCOME",52_000_00L,1_790_000_000_000L,"SYNTHETIC01")
    private val receipt=EvidenceCanonicalFact("account-mpesa","INCOME",52_000_00L,1_790_000_000_000L,"SYNTHETIC01","sms")

    @Test fun sharedUniqueReferenceSameAccountAmountAndDirectionIsCandidate() {
        assertTrue(EvidenceLinkSafetyPolicy.canLink(statement,receipt,"REFERENCE"))
        assertTrue(EvidenceLinkSafetyPolicy.canLink(statement,receipt.copy(transactionReference=" synthetic01 "),"REFERENCE"))
    }
    @Test fun amountDayAloneCannotMergeSeparateDeposits() {
        assertFalse(EvidenceLinkSafetyPolicy.canLink(statement.copy(transactionReference=null),
            receipt.copy(transactionReference=null),"REFERENCE"))
    }
    @Test fun changedAccountDirectionAmountOrReferenceRefusesLink() {
        assertFalse(EvidenceLinkSafetyPolicy.canLink(statement,receipt.copy(accountId="account-cash"),"REFERENCE"))
        assertFalse(EvidenceLinkSafetyPolicy.canLink(statement,receipt.copy(direction="EXPENSE"),"REFERENCE"))
        assertFalse(EvidenceLinkSafetyPolicy.canLink(statement,receipt.copy(amountMinor=52_000_01L),"REFERENCE"))
        assertFalse(EvidenceLinkSafetyPolicy.canLink(statement,receipt.copy(transactionReference="ANOTHER001"),"REFERENCE"))
    }
    @Test fun outsideDateWindowCannotLink() {
        assertFalse(EvidenceLinkSafetyPolicy.canLink(statement,receipt.copy(
            occurredAtEpochMs=receipt.occurredAtEpochMs+73L*60L*60L*1000L),"REFERENCE"))
    }
    @Test fun createdRecordRequiresExactTimeButNoReference() {
        val manual=statement.copy(transactionReference=null)
        val inserted=receipt.copy(transactionReference=null,sourceKind="manual_entry")
        assertTrue(EvidenceLinkSafetyPolicy.canLink(manual,inserted,"CREATED"))
        assertFalse(EvidenceLinkSafetyPolicy.canLink(manual,inserted.copy(
            occurredAtEpochMs=receipt.occurredAtEpochMs+1),"CREATED"))
        assertFalse(EvidenceLinkSafetyPolicy.canLink(manual,inserted.copy(sourceKind="sms"),"CREATED"))
    }
    @Test fun ownTransferIsPossibleOutgoingObservationNotExtraIncome() {
        val outgoing=statement.copy(direction="EXPENSE")
        assertTrue(EvidenceLinkSafetyPolicy.canLink(outgoing,receipt.copy(direction="TRANSFER"),"REFERENCE"))
        assertFalse(EvidenceLinkSafetyPolicy.canLink(statement,receipt.copy(direction="TRANSFER"),"REFERENCE"))
    }
    @Test fun zeroNegativeOverflowOrUnsupportedReasonsAreRejected() {
        assertFalse(EvidenceLinkSafetyPolicy.canLink(statement.copy(amountMinor=0),receipt,"REFERENCE"))
        assertFalse(EvidenceLinkSafetyPolicy.canLink(statement.copy(occurredAtEpochMs=0),receipt,"REFERENCE"))
        assertFalse(EvidenceLinkSafetyPolicy.canLink(statement,receipt,"AMOUNT_ONLY"))
        assertFalse(EvidenceLinkSafetyPolicy.canLink(statement.copy(accountId=""),receipt,"REFERENCE"))
    }
}

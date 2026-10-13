package com.kedevjimmy.pesatrack.domain.receivables

import org.junit.Assert.*
import org.junit.Test

class ReceivableNotificationPolicyTest {
    private val now = 10_000_000L
    private val people = listOf(NoticePerson("one","0712345678",true,200_000))
    private fun match(phone: String? = "+254712345678", people: List<NoticePerson> = this.people,
        occurred: Long = now - 1000, received: Long = now - 1000, source: String = "sms",
        direction: String = "INCOME", eligible: Boolean = true, ambiguous: Boolean = false,
        amount: Long = 50_000) = ReceivableNotificationPolicy.match(people,phone,direction,amount,
            occurred,received,source,now - 5000,now,eligible,ambiguous)
    @Test fun uniquePhoneAllowsPartialRepaymentAndOutgoingLoan() {
        assertEquals("one",match()?.id)
        assertEquals("one",match(direction = "EXPENSE")?.id)
    }
    @Test fun namesOrConflictingPhonesAndCollisionsNeverGuessPerson() {
        assertNull(match(phone = null)); assertNull(match(phone = "0712345679"))
        assertNull(match(people = people + people.first().copy(id = "two")))
    }
    @Test fun historicalReplayRefundTransferAndDuplicateNeverPrompt() {
        assertNull(match(source = "historical_sms")); assertNull(match(source = "statement"))
        assertNull(match(occurred = now - 6000)); assertNull(match(received = now - 6000))
        assertNull(match(eligible = false)); assertNull(match(ambiguous = true))
        assertNull(match(direction = "TRANSFER")); assertNull(match(amount = 0))
        assertNull(match(occurred = now + 1))
    }
    @Test fun settledHistoryCanSuggestReviewButCannotOfferRepayment() {
        val settled = people.map { it.copy(outstanding = 0) }
        assertNotNull(match(people = settled))
        assertFalse(ReceivableNotificationPolicy.canRepay(settled.first(),50_000))
        assertTrue(ReceivableNotificationPolicy.canRepay(people.first(),50_000))
        assertFalse(ReceivableNotificationPolicy.canRepay(people.first(),250_000))
    }
    @Test fun unconfirmedOrInvalidHistoryIsNotDebtEvidence() {
        assertNull(match(people = people.map { it.copy(hasHistory = false) }))
        assertNull(match(people = people.map { it.copy(outstanding = null) }))
    }
    @Test fun historyAddedAfterThePaymentDoesNotRetroactivelyPrompt() {
        assertNull(match(people = people.map { it.copy(knownAt = now) }))
        assertNotNull(match(people = people.map { it.copy(knownAt = now - 2000) }))
    }
}

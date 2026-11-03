package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.assertEquals
import org.junit.Test

class StatementAccountConsentPolicyTest {
    @Test fun explicitB2RejectionOverridesLegacyOwnershipHint() {
        val allowed = StatementAccountConsentPolicy.confirmedAccountIds(
            legacyConfirmed = setOf("legacy", "conflicted", "historical"),
            b2Confirmed = setOf("current"),
            b2Rejected = setOf("conflicted", "historical")
        )
        assertEquals(setOf("legacy", "current"), allowed)
    }

    @Test fun historicalLegacyConfirmationWorksUntilOwnerRejectsIt() {
        assertEquals(setOf("legacy"), StatementAccountConsentPolicy.confirmedAccountIds(
            setOf("legacy"), emptySet(), emptySet()))
        assertEquals(emptySet<String>(), StatementAccountConsentPolicy.confirmedAccountIds(
            setOf("legacy"), emptySet(), setOf("legacy")))
    }

    @Test fun b2ReconfirmationRestoresEligibilityOnceRejectionIsWithdrawn() {
        val confirmed = setOf("account")
        assertEquals(confirmed, StatementAccountConsentPolicy.confirmedAccountIds(
            emptySet(), confirmed, emptySet()))
    }
}

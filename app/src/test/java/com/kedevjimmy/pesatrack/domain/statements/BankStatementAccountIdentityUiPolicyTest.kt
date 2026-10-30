package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankStatementAccountIdentityUiPolicyTest {
    @Test fun preservesLeadingZeroesWhenSavingCompleteBankNumber() {
        assertEquals("001234567890", BankStatementAccountIdentityUiPolicy.normalizeFullNumber("00 1234-567890"))
        assertTrue(BankStatementAccountIdentityUiPolicy.hasFullNumber("001234567890"))
    }

    @Test fun neverTreatsMissingPartialMaskedOrMalformedNumberAsComplete() {
        listOf(null, "", "1234", "****7890", "1234567", "ABC123456789", "12345678/9",
            "123456789012345678901").forEach { value ->
            assertNull(BankStatementAccountIdentityUiPolicy.normalizeFullNumber(value))
            assertFalse(BankStatementAccountIdentityUiPolicy.hasFullNumber(value))
        }
    }

    @Test fun unknownIdentityErrorExplainsHowToResolveItWithoutPrivateData() {
        val message = BankStatementAccountIdentityUiPolicy.safeBlockedMessage(
            StatementAdmissionResult.Reason.UNKNOWN_ACCOUNT_IDENTITY
        )
        assertTrue(message.contains("complete bank account number", ignoreCase = true))
        assertTrue(message.contains("Nothing was saved"))
        assertFalse(message.contains("UNKNOWN_ACCOUNT_IDENTITY"))
    }

    @Test fun wrongAccountGuidanceDoesNotShowDocumentIdentifiers() {
        val message = BankStatementAccountIdentityUiPolicy.safeBlockedMessage(
            StatementAdmissionResult.Reason.WRONG_ACCOUNT
        )
        assertTrue(message.contains("does not match", ignoreCase = true))
        assertTrue(message.contains("Nothing was saved"))
        assertFalse(message.contains("WRONG_ACCOUNT"))
    }
}

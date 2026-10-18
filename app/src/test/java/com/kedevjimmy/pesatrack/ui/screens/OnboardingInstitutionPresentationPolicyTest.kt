package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.domain.interpretation.AccountOwnershipHint
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountNature
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingInstitutionPresentationPolicyTest {
    @Test fun topFiveAreRankedByObservedMessageCountAndNeverIncludeSafaricomProducts() {
        val candidates = listOf(
            source("Equity Bank", "KENYAN_BANK_EQUITY", 9),
            source("M-Pesa", "SAFARICOM_MPESA", 3),
            source("M-Shwari", "NCBA_MSHWARI_SAVINGS", 25),
            source("KCB M-Pesa", "KCB_MPESA_SAVINGS", 20),
            source("Absa", "KENYAN_BANK_ABSA", 7),
            source("Co-operative Bank", "KENYAN_BANK_COOPERATIVE", 5),
            source("NCBA", "KENYAN_BANK_NCBA", 2),
            source("Family Bank", "KENYAN_BANK_FAMILY", 1)
        )

        val visible = OnboardingInstitutionPresentationPolicy.topFive(candidates)

        assertEquals(
            listOf("Equity Bank", "Absa", "Co-operative Bank", "M-Pesa", "NCBA"),
            visible.map { it.name }
        )
        assertTrue(visible.none { it.reference.providerKey in OnboardingInstitutionPresentationPolicy.safaricomProductProviderKeys })
        assertEquals(5, visible.size)
    }

    @Test fun maskedMpesaIdentifierShowsOnlyTheLastThreeDigits() {
        assertEquals("••• 678", OnboardingInstitutionPresentationPolicy.maskIdentifier("+254712345678"))
        assertNull(OnboardingInstitutionPresentationPolicy.maskIdentifier("12"))
        assertNull(OnboardingInstitutionPresentationPolicy.maskIdentifier(null))
    }

    private fun source(name: String, providerKey: String, messageCount: Int): DetectedSetupSource {
        val product = providerKey in OnboardingInstitutionPresentationPolicy.safaricomProductProviderKeys
        val reference = FinancialAccountReference(
            entityKey = "provider:$providerKey",
            displayName = name,
            providerKey = providerKey,
            accountTypeHint = if (product) AccountType.SAVINGS else AccountType.BANK,
            ownershipHint = AccountOwnershipHint.UNKNOWN,
            nature = FinancialAccountNature.ASSET
        )
        return DetectedSetupSource(name, if (product) "Savings" else "Bank", messageCount, reference)
    }
}

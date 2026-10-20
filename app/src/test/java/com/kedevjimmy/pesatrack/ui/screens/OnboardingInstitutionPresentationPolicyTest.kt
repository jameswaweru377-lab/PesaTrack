package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.domain.interpretation.AccountOwnershipHint
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountNature
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialAccountReference
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnostic
import com.kedevjimmy.pesatrack.ingestion.FinancialIngestionDiagnosticCode
import com.kedevjimmy.pesatrack.domain.messagepack.MessageParseRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingInstitutionPresentationPolicyTest {
    @Test fun repeatedImportObservationsDoNotInflateInstitutionMessageCount() {
        val original = diagnostic(
            title = "EQUITY",
            receivedAt = 1_800_000_000_000L,
            shapeId = "shape-1"
        )

        assertEquals(
            2,
            OnboardingInstitutionPresentationPolicy.uniqueObservationCount(
                listOf(original, original.copy(), diagnostic("EQUITY", 1_800_000_001_000L, "shape-1"))
            )
        )
    }

    @Test fun observationsWithoutStableShapeIdentityAreNotCollapsed() {
        val diagnostic = diagnostic("EQUITY", 1_800_000_000_000L, null)

        assertEquals(
            2,
            OnboardingInstitutionPresentationPolicy.uniqueObservationCount(
                listOf(diagnostic, diagnostic.copy())
            )
        )
    }

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

    private fun diagnostic(
        title: String,
        receivedAt: Long,
        shapeId: String?
    ) = FinancialIngestionDiagnostic(
        title = title,
        sourceDisplayName = null,
        sourceType = "Bank",
        eventLabel = "Financial message",
        sourcePackage = null,
        sourceKind = "historical_sms",
        receivedAtEpochMs = receivedAt,
        code = FinancialIngestionDiagnosticCode.LINKED_OBSERVATION,
        insertedTransactions = 0,
        insertedFinancialEvents = 0,
        pendingReviewEvents = 0,
        linkedTransactionId = 1L,
        parseRoute = MessageParseRoute.TEMPLATE,
        messageShapeId = shapeId
    )
}

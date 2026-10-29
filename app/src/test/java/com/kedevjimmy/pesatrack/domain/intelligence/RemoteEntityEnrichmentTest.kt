package com.kedevjimmy.pesatrack.domain.intelligence

import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteEntityEnrichmentTest {
    @Test
    fun androidSanitizerFollowsSharedWorkerPrivacyCorpus() {
        val fixture = javaClass.classLoader
            ?.getResourceAsStream("merchant-descriptor-privacy-v1.json")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: error("Shared descriptor privacy corpus missing")
        val examples = JsonParser.parseString(fixture).asJsonArray
        assertTrue("Expected a meaningful privacy corpus", examples.size() >= 12)
        for (row in examples) {
            val example = row.asJsonObject
            val input = example.get("input").asString
            val expected = example.get("expected")
                .takeUnless { it.isJsonNull }
                ?.asString
            assertEquals(
                "Android descriptor filter disagrees for $input",
                expected,
                EntityDescriptorPrivacyPolicy.sanitize(input)
            )
        }
    }

    @Test
    fun sanitizerAllowsCommercialDescriptor() {
        assertEquals(
            "ACME HARDWARE LTD",
            EntityDescriptorPrivacyPolicy
                .sanitize(
                    "  ACME HARDWARE LTD  "
                )
        )
    }

    @Test
    fun sanitizerBlocksLikelyPersonalName() {
        assertNull(
            EntityDescriptorPrivacyPolicy
                .sanitize("JOHN KAMAU")
        )
    }

    @Test
    fun sanitizerBlocksPhoneNumber() {
        assertNull(
            EntityDescriptorPrivacyPolicy
                .sanitize(
                    "JOHN 0712345678"
                )
        )
    }

    @Test
    fun sanitizerBlocksTransactionCode() {
        assertNull(
            EntityDescriptorPrivacyPolicy
                .sanitize(
                    "ABC STORE TQK12ABC34"
                )
        )
    }

    @Test
    fun safeExternalCategoryCanAutoReview() {
        val result =
            RemoteEntityEnrichment(
                canonicalName = "Acme Market",
                domain = "example.com",
                logoUrl = null,
                categoryHint =
                    TransactionCategory.GROCERIES,
                confidence =
                    EntityMatchConfidence.HIGH,
                source = "test"
            )

        assertTrue(
            RemoteEntityAutoReviewPolicy
                .shouldAutoReview(result)
        )
    }

    @Test
    fun likelyConfidenceCannotAutoReview() {
        val result =
            RemoteEntityEnrichment(
                canonicalName = "Acme Market",
                domain = "example.com",
                logoUrl = null,
                categoryHint =
                    TransactionCategory.GROCERIES,
                confidence =
                    EntityMatchConfidence.LIKELY,
                source = "test"
            )

        assertFalse(
            RemoteEntityAutoReviewPolicy
                .shouldAutoReview(result)
        )
    }

    @Test
    fun financeStyleCategoryIsNotAutoMapped() {
        assertNull(
            RemoteEntityAutoReviewPolicy
                .categoryFromExternalLabel(
                    "Financial Services"
                )
        )
    }

    @Test
    fun foodAndDrinkMapsToEatingOut() {
        assertEquals(
            TransactionCategory.EATING_OUT,
            RemoteEntityAutoReviewPolicy
                .categoryFromExternalLabel(
                    "Food & Drink"
                )
        )
    }

    @Test
    fun paymentProcessorIsRejectedAsAutomaticSalarySource() {
        val result =
            RemoteEntityEnrichment(
                canonicalName =
                    "Sample Payments Ltd",
                domain =
                    "payments.example",
                logoUrl = null,
                categoryHint = null,
                confidence =
                    EntityMatchConfidence.HIGH,
                source = "test",
                organizationRole =
                    RemoteOrganizationRole
                        .PAYMENT_PROCESSOR,
                salaryCandidateSuitability =
                    SalaryCandidateSuitability
                        .REJECT
            )

        assertTrue(
            SalaryCandidateRemotePolicy
                .shouldReject(result)
        )
    }

    @Test
    fun ordinaryFinancialInstitutionIsNotAutomaticallyRejectedAsEmployer() {
        val result =
            RemoteEntityEnrichment(
                canonicalName =
                    "Sample Bank Ltd",
                domain =
                    "bank.example",
                logoUrl = null,
                categoryHint = null,
                confidence =
                    EntityMatchConfidence.HIGH,
                source = "test",
                organizationRole =
                    RemoteOrganizationRole
                        .FINANCIAL_INSTITUTION,
                salaryCandidateSuitability =
                    SalaryCandidateSuitability
                        .REVIEW
            )

        assertFalse(
            SalaryCandidateRemotePolicy
                .shouldReject(result)
        )
    }

    @Test
    fun onlyMerchantAndBillChannelsMayGoOnline() {
        assertTrue(
            RemoteEntityEligibilityPolicy
                .canSendOnline(
                    MpesaTransactionType
                        .PAY_BILL
                        .name
                )
        )
        assertTrue(
            RemoteEntityEligibilityPolicy
                .canSendOnline(
                    MpesaTransactionType
                        .BUY_GOODS
                        .name
                )
        )
        assertFalse(
            RemoteEntityEligibilityPolicy
                .canSendOnline(
                    MpesaTransactionType
                        .SENT_MONEY
                        .name
                )
        )
        assertFalse(
            RemoteEntityEligibilityPolicy
                .canSendOnline(
                    MpesaTransactionType
                        .RECEIVED
                        .name
                )
        )
    }
}

package com.kedevjimmy.pesatrack.domain.intelligence

import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteEntityEnrichmentTest {
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
}

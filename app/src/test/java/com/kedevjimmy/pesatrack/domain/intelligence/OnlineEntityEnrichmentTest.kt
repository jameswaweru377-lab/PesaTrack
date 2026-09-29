package com.kedevjimmy.pesatrack.domain.intelligence

import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineEntityEnrichmentTest {
    @Test
    fun sanitizerOnlyAllowsMerchantAndBillDescriptors() {
        assertNull(
            EntityDescriptorSanitizer.from(
                type =
                    MpesaTransactionType.SENT_MONEY,
                counterparty = "JOHN KAMAU"
            )
        )

        val descriptor =
            EntityDescriptorSanitizer.from(
                type =
                    MpesaTransactionType.BUY_GOODS,
                counterparty =
                    "Acme Market 0712345678 TQK12ABC34"
            )

        assertEquals(
            "Acme Market",
            descriptor?.value
        )
    }

    @Test
    fun financeCategoryNeverBecomesExpenseAutomatically() {
        assertNull(
            OnlineEntityCategoryPolicy.map(
                remoteCategory = "Finance",
                canonicalName = "Example Bank"
            )
        )
    }

    @Test
    fun genericUtilityNeedsElectricityOrWaterMeaning() {
        assertEquals(
            TransactionCategory.ELECTRICITY,
            OnlineEntityCategoryPolicy.map(
                remoteCategory = "Utilities",
                canonicalName = "Example Power"
            )
        )
        assertEquals(
            TransactionCategory.WATER,
            OnlineEntityCategoryPolicy.map(
                remoteCategory = "Utilities",
                canonicalName = "Example Water"
            )
        )
        assertNull(
            OnlineEntityCategoryPolicy.map(
                remoteCategory = "Utilities",
                canonicalName = "Example Utility"
            )
        )
    }

    @Test
    fun resolvedFoodMerchantCanAutoReviewBuyGoods() {
        val result =
            OnlineEntityAutoReviewPolicy
                .classify(
                    type =
                        MpesaTransactionType
                            .BUY_GOODS,
                    profile =
                        OnlineEntityProfile(
                            canonicalName =
                                "Example Cafe",
                            domain =
                                "example.com",
                            logoUrl = null,
                            remoteCategory =
                                "Food & Drink",
                            confidence =
                                EntityMatchConfidence.HIGH,
                            source = "test"
                        )
                )

        assertEquals(
            TransactionCategory.EATING_OUT,
            result?.category
        )
        assertEquals(
            ReviewState.AUTO_CLASSIFIED,
            result?.reviewState
        )
        assertTrue(
            result?.evidenceSource
                ?.startsWith(
                    "entity_intelligence:online:"
                ) == true
        )
    }

    @Test
    fun likelyRemoteMatchStaysOutOfAutoReview() {
        assertNull(
            OnlineEntityAutoReviewPolicy
                .classify(
                    type =
                        MpesaTransactionType
                            .PAY_BILL,
                    profile =
                        OnlineEntityProfile(
                            canonicalName =
                                "Example Power",
                            domain =
                                "example.com",
                            logoUrl = null,
                            remoteCategory =
                                "Utilities",
                            confidence =
                                EntityMatchConfidence
                                    .LIKELY,
                            source = "test"
                        )
                )
        )
    }
}

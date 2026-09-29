package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewTriagePolicyTest {
    @Test
    fun phoneIdentityIsPreferredOverName() {
        assertEquals(
            "phone:+254712345678",
            ReviewTriagePolicy.identityKey(
                counterparty = "Joseph Mwango",
                counterpartyPhone = "0712345678"
            )
        )
    }

    @Test
    fun fullNameIsPreservedAsTheIdentity() {
        assertEquals(
            "name:joseph mwango",
            ReviewTriagePolicy.identityKey(
                counterparty = "  JOSEPH   MWANGO ",
                counterpartyPhone = null
            )
        )
    }

    @Test
    fun oneOccurrenceDefaultsToOther() {
        assertTrue(ReviewTriagePolicy.shouldDefaultToOther(1))
        assertFalse(ReviewTriagePolicy.shouldRequestTeaching(1))
    }

    @Test
    fun repeatedIdentityRequestsTeaching() {
        assertFalse(ReviewTriagePolicy.shouldDefaultToOther(3))
        assertTrue(ReviewTriagePolicy.shouldRequestTeaching(3))
    }
    @Test
    fun onlyUnresolvedCategoriesEnterIdentityTriage() {
        assertTrue(
            ReviewTriagePolicy
                .shouldTriageCategory(
                    TransactionCategory
                        .UNCATEGORIZED.name
                )
        )
        assertTrue(
            ReviewTriagePolicy
                .shouldTriageCategory(
                    TransactionCategory.OTHER.name
                )
        )
        assertFalse(
            ReviewTriagePolicy
                .shouldTriageCategory(
                    TransactionCategory
                        .ELECTRICITY.name
                )
        )
        assertFalse(
            ReviewTriagePolicy
                .shouldTriageCategory(
                    TransactionCategory
                        .GROCERIES.name
                )
        )
    }

}

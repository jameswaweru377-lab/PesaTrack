package com.kedevjimmy.pesatrack.domain.learning

import com.kedevjimmy.pesatrack.domain.identity.IdentityConfidence
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalLearningPolicyTest {
    @Test
    fun userConfirmedIdentityIsHighConfidence() {
        assertEquals(
            PersonalLearningConfidence.HIGH,
            PersonalLearningPolicy.confidenceFor(
                identityConfidence =
                    IdentityConfidence.LOW,
                identityUserConfirmed = true
            )
        )
    }

    @Test
    fun highIdentityEvidenceAutoApplies() {
        val confidence =
            PersonalLearningPolicy.confidenceFor(
                identityConfidence =
                    IdentityConfidence.HIGH,
                identityUserConfirmed = false
            )

        assertEquals(
            PersonalLearningAction.AUTO_APPLY,
            PersonalLearningPolicy
                .actionFor(confidence)
        )
    }

    @Test
    fun mediumIdentityEvidenceOnlySuggests() {
        val confidence =
            PersonalLearningPolicy.confidenceFor(
                identityConfidence =
                    IdentityConfidence.MEDIUM,
                identityUserConfirmed = false
            )

        assertEquals(
            PersonalLearningAction.SUGGEST,
            PersonalLearningPolicy
                .actionFor(confidence)
        )
    }

    @Test
    fun lowIdentityEvidenceStillAsks() {
        val confidence =
            PersonalLearningPolicy.confidenceFor(
                identityConfidence =
                    IdentityConfidence.LOW,
                identityUserConfirmed = false
            )

        assertEquals(
            PersonalLearningAction.ASK,
            PersonalLearningPolicy
                .actionFor(confidence)
        )
    }

    @Test
    fun onlyOrdinaryExpenseCategoriesAreLearnable() {
        assertTrue(
            PersonalLearningPolicy
                .isLearnableCategory(
                    categoryId =
                        TransactionCategory
                            .TRANSPORT.name,
                    direction =
                        MoneyDirection.EXPENSE
                )
        )
        assertFalse(
            PersonalLearningPolicy
                .isLearnableCategory(
                    categoryId =
                        TransactionCategory
                            .TRANSFER.name,
                    direction =
                        MoneyDirection.TRANSFER
                )
        )
        assertFalse(
            PersonalLearningPolicy
                .isLearnableCategory(
                    categoryId =
                        TransactionCategory
                            .DEBT_LOAN.name,
                    direction =
                        MoneyDirection.EXPENSE
                )
        )
    }
}

package com.kedevjimmy.pesatrack.domain.learning

import com.kedevjimmy.pesatrack.domain.identity.IdentityConfidence
import com.kedevjimmy.pesatrack.domain.identity.IdentityNodeKind
import com.kedevjimmy.pesatrack.domain.identity.IdentityOwnership
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalLearningPolicyTest {
    @Test
    fun oneConsistentReviewStillAsks() {
        val result =
            PersonalLearningPolicy.recommend(
                identityId = "business:sample",
                direction =
                    MoneyDirection.EXPENSE,
                evidence =
                    listOf(
                        PersonalCategoryEvidence(
                            categoryId =
                                TransactionCategory
                                    .GROCERIES.name,
                            supportCount = 1
                        )
                    ),
                trustedForAutoApply = true
            )

        assertEquals(
            PersonalLearningConfidence.LOW,
            result?.confidence
        )
        assertEquals(
            PersonalLearningAction.ASK,
            result?.action
        )
    }

    @Test
    fun twoConsistentReviewsSuggestButDoNotAutoApply() {
        val result =
            PersonalLearningPolicy.recommend(
                identityId = "business:sample",
                direction =
                    MoneyDirection.EXPENSE,
                evidence =
                    listOf(
                        PersonalCategoryEvidence(
                            categoryId =
                                TransactionCategory
                                    .GROCERIES.name,
                            supportCount = 2
                        )
                    ),
                trustedForAutoApply = true
            )

        assertEquals(
            PersonalLearningConfidence.MEDIUM,
            result?.confidence
        )
        assertEquals(
            PersonalLearningAction.SUGGEST,
            result?.action
        )
    }

    @Test
    fun threeConsistentReviewsAutoApplyOnlyForTrustedIdentity() {
        val evidence =
            listOf(
                PersonalCategoryEvidence(
                    categoryId =
                        TransactionCategory
                            .TRANSPORT.name,
                    supportCount = 3
                )
            )

        val trusted =
            PersonalLearningPolicy.recommend(
                identityId = "business:trusted",
                direction =
                    MoneyDirection.EXPENSE,
                evidence = evidence,
                trustedForAutoApply = true
            )
        val untrusted =
            PersonalLearningPolicy.recommend(
                identityId = "business:untrusted",
                direction =
                    MoneyDirection.EXPENSE,
                evidence = evidence,
                trustedForAutoApply = false
            )

        assertEquals(
            PersonalLearningConfidence.HIGH,
            trusted?.confidence
        )
        assertEquals(
            PersonalLearningAction.AUTO_APPLY,
            trusted?.action
        )
        assertEquals(
            PersonalLearningConfidence.HIGH,
            untrusted?.confidence
        )
        assertEquals(
            PersonalLearningAction.SUGGEST,
            untrusted?.action
        )
    }

    @Test
    fun conflictingEvidenceCannotReachHighConfidenceAtSeventyFivePercent() {
        val result =
            PersonalLearningPolicy.recommend(
                identityId = "person:sample",
                direction =
                    MoneyDirection.EXPENSE,
                evidence =
                    listOf(
                        PersonalCategoryEvidence(
                            categoryId =
                                TransactionCategory
                                    .TRANSPORT.name,
                            supportCount = 3
                        ),
                        PersonalCategoryEvidence(
                            categoryId =
                                TransactionCategory
                                    .FAMILY.name,
                            supportCount = 1
                        )
                    ),
                trustedForAutoApply = true
            )

        assertEquals(
            PersonalLearningConfidence.MEDIUM,
            result?.confidence
        )
        assertEquals(
            PersonalLearningAction.SUGGEST,
            result?.action
        )
        assertEquals(
            4,
            result?.totalReviewCount
        )
    }

    @Test
    fun onlyExternalResolvedIdentitiesCanLearn() {
        assertTrue(
            PersonalLearningPolicy
                .isLearnableIdentity(
                    kind =
                        IdentityNodeKind.BUSINESS,
                    ownership =
                        IdentityOwnership.EXTERNAL
                )
        )
        assertFalse(
            PersonalLearningPolicy
                .isLearnableIdentity(
                    kind =
                        IdentityNodeKind.UNKNOWN,
                    ownership =
                        IdentityOwnership.EXTERNAL
                )
        )
        assertFalse(
            PersonalLearningPolicy
                .isLearnableIdentity(
                    kind =
                        IdentityNodeKind.SELF,
                    ownership =
                        IdentityOwnership.SELF
                )
        )
    }

    @Test
    fun trustedIdentityRequiresStrongOrUserConfirmedIdentityEvidence() {
        assertTrue(
            PersonalLearningPolicy
                .isTrustedForAutoApply(
                    kind =
                        IdentityNodeKind.BUSINESS,
                    ownership =
                        IdentityOwnership.EXTERNAL,
                    confidence =
                        IdentityConfidence.HIGH,
                    userConfirmed = false
                )
        )
        assertTrue(
            PersonalLearningPolicy
                .isTrustedForAutoApply(
                    kind =
                        IdentityNodeKind.PERSON,
                    ownership =
                        IdentityOwnership.EXTERNAL,
                    confidence =
                        IdentityConfidence.LOW,
                    userConfirmed = true
                )
        )
        assertFalse(
            PersonalLearningPolicy
                .isTrustedForAutoApply(
                    kind =
                        IdentityNodeKind.PERSON,
                    ownership =
                        IdentityOwnership.EXTERNAL,
                    confidence =
                        IdentityConfidence.LOW,
                    userConfirmed = false
                )
        )
    }

    @Test
    fun structuralCategoriesAreNeverLearningEvidence() {
        assertTrue(
            PersonalLearningPolicy
                .isLearnableCategory(
                    categoryId =
                        TransactionCategory
                            .GROCERIES.name,
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
        assertFalse(
            PersonalLearningPolicy
                .isLearnableCategory(
                    categoryId =
                        TransactionCategory
                            .OTHER.name,
                    direction =
                        MoneyDirection.EXPENSE
                )
        )
    }

    @Test
    fun predictionTargetProtectsRulesRelationsAndUserOverrides() {
        assertTrue(
            PersonalLearningPolicy
                .isEligiblePredictionTarget(
                    type = "BUY_GOODS",
                    direction = "EXPENSE",
                    reviewState =
                        "NEEDS_REVIEW",
                    ruleId = null,
                    relationType = null,
                    currentCategoryId =
                        TransactionCategory
                            .UNCATEGORIZED.name
                )
        )
        assertFalse(
            PersonalLearningPolicy
                .isEligiblePredictionTarget(
                    type = "BUY_GOODS",
                    direction = "EXPENSE",
                    reviewState =
                        "USER_CLASSIFIED",
                    ruleId = null,
                    relationType = null,
                    currentCategoryId =
                        TransactionCategory
                            .UNCATEGORIZED.name
                )
        )
        assertFalse(
            PersonalLearningPolicy
                .isEligiblePredictionTarget(
                    type = "BUY_GOODS",
                    direction = "EXPENSE",
                    reviewState =
                        "NEEDS_REVIEW",
                    ruleId = "rule-1",
                    relationType = null,
                    currentCategoryId =
                        TransactionCategory
                            .UNCATEGORIZED.name
                )
        )
        assertFalse(
            PersonalLearningPolicy
                .isEligiblePredictionTarget(
                    type = "BUY_GOODS",
                    direction = "EXPENSE",
                    reviewState =
                        "NEEDS_REVIEW",
                    ruleId = null,
                    relationType =
                        "DEBT_REPAYMENT",
                    currentCategoryId =
                        TransactionCategory
                            .UNCATEGORIZED.name
                )
        )
    }
}

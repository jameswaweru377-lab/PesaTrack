package com.kedevjimmy.pesatrack.domain.learning

import com.kedevjimmy.pesatrack.domain.identity.IdentityConfidence
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory

object PersonalLearningPolicy {
    fun confidenceFor(
        identityConfidence: IdentityConfidence,
        identityUserConfirmed: Boolean
    ): PersonalLearningConfidence =
        when {
            identityUserConfirmed ->
                PersonalLearningConfidence.HIGH

            identityConfidence ==
                IdentityConfidence.USER_CONFIRMED ||
                identityConfidence ==
                IdentityConfidence.HIGH ->
                PersonalLearningConfidence.HIGH

            identityConfidence ==
                IdentityConfidence.MEDIUM ->
                PersonalLearningConfidence.MEDIUM

            else ->
                PersonalLearningConfidence.LOW
        }

    fun actionFor(
        confidence: PersonalLearningConfidence
    ): PersonalLearningAction =
        when (confidence) {
            PersonalLearningConfidence.HIGH ->
                PersonalLearningAction.AUTO_APPLY

            PersonalLearningConfidence.MEDIUM ->
                PersonalLearningAction.SUGGEST

            PersonalLearningConfidence.LOW ->
                PersonalLearningAction.ASK
        }

    fun isLearnableCategory(
        categoryId: String,
        direction: MoneyDirection
    ): Boolean {
        if (direction != MoneyDirection.EXPENSE) {
            return false
        }

        return categoryId !in STRUCTURAL_CATEGORIES
    }

    fun correctionAction(
        categoryId: String,
        direction: MoneyDirection
    ): PersonalLearningCorrectionAction =
        if (
            isLearnableCategory(
                categoryId = categoryId,
                direction = direction
            )
        ) {
            PersonalLearningCorrectionAction
                .UPDATE_PREFERENCE
        } else {
            PersonalLearningCorrectionAction
                .FORGET_PREFERENCE
        }

    private val STRUCTURAL_CATEGORIES =
        setOf(
            TransactionCategory.UNCATEGORIZED.name,
            TransactionCategory.OTHER.name,
            TransactionCategory.TRANSFER.name,
            TransactionCategory.INCOME.name,
            TransactionCategory.DEBT_LOAN.name,
            TransactionCategory.SAVINGS.name,
            TransactionCategory.INVESTMENT.name,
            TransactionCategory.CASH.name
        )
}

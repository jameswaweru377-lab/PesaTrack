package com.kedevjimmy.pesatrack.domain.learning

import com.kedevjimmy.pesatrack.domain.identity.IdentityConfidence
import com.kedevjimmy.pesatrack.domain.identity.IdentityNodeKind
import com.kedevjimmy.pesatrack.domain.identity.IdentityOwnership
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection

object PersonalLearningPolicy {
    const val MEDIUM_MIN_SUPPORT = 2
    const val HIGH_MIN_SUPPORT = 3
    const val MEDIUM_MIN_RATIO = 0.67
    const val HIGH_MIN_RATIO = 0.80

    private val blockedLearnedCategoryIds =
        setOf(
            "UNCATEGORIZED",
            "OTHER",
            "TRANSFER",
            "INCOME",
            "DEBT_LOAN",
            "SAVINGS",
            "INVESTMENT",
            "CASH"
        )

    private val protectedCurrentCategoryIds =
        setOf(
            "TRANSFER",
            "INCOME",
            "DEBT_LOAN",
            "SAVINGS",
            "INVESTMENT",
            "CASH"
        )

    private val ordinaryOutgoingTypes =
        setOf(
            "SENT_MONEY",
            "PAY_BILL",
            "BUY_GOODS"
        )

    private val externalKinds =
        setOf(
            IdentityNodeKind.PERSON,
            IdentityNodeKind.BUSINESS,
            IdentityNodeKind.BILLER,
            IdentityNodeKind.FINANCIAL_INSTITUTION
        )

    fun isLearnableCategory(
        categoryId: String,
        direction: MoneyDirection
    ): Boolean =
        direction == MoneyDirection.EXPENSE &&
            categoryId.trim()
                .uppercase() !in
            blockedLearnedCategoryIds

    fun isLearnableIdentity(
        kind: IdentityNodeKind,
        ownership: IdentityOwnership
    ): Boolean =
        ownership ==
            IdentityOwnership.EXTERNAL &&
            kind in externalKinds

    fun isTrustedForAutoApply(
        kind: IdentityNodeKind,
        ownership: IdentityOwnership,
        confidence: IdentityConfidence,
        userConfirmed: Boolean
    ): Boolean =
        isLearnableIdentity(
            kind = kind,
            ownership = ownership
        ) &&
            (
                userConfirmed ||
                    confidence ==
                        IdentityConfidence.HIGH ||
                    confidence ==
                        IdentityConfidence.USER_CONFIRMED
                )

    fun isEligibleReviewEvidence(
        type: String,
        direction: MoneyDirection,
        ruleId: String?,
        relationType: String?,
        categoryId: String
    ): Boolean =
        type in ordinaryOutgoingTypes &&
            ruleId == null &&
            relationType == null &&
            isLearnableCategory(
                categoryId = categoryId,
                direction = direction
            )

    fun isEligiblePredictionTarget(
        type: String,
        direction: String,
        reviewState: String,
        ruleId: String?,
        relationType: String?,
        currentCategoryId: String
    ): Boolean =
        type in ordinaryOutgoingTypes &&
            direction ==
                MoneyDirection.EXPENSE.name &&
            reviewState != "USER_CLASSIFIED" &&
            reviewState != "EXCLUDED" &&
            ruleId == null &&
            relationType == null &&
            currentCategoryId !in
                protectedCurrentCategoryIds

    fun recommend(
        identityId: String,
        direction: MoneyDirection,
        evidence: List<PersonalCategoryEvidence>,
        trustedForAutoApply: Boolean
    ): PersonalLearningSuggestion? {
        val usable =
            evidence
                .filter {
                    it.supportCount > 0 &&
                        isLearnableCategory(
                            categoryId =
                                it.categoryId,
                            direction =
                                direction
                        )
                }
                .sortedWith(
                    compareByDescending<
                        PersonalCategoryEvidence
                    > {
                        it.supportCount
                    }.thenBy {
                        it.categoryId
                    }
                )

        val strongest =
            usable.firstOrNull()
                ?: return null
        val total =
            usable.sumOf {
                it.supportCount
            }
        if (total <= 0) {
            return null
        }

        val agreement =
            strongest.supportCount
                .toDouble() /
                total.toDouble()

        val confidence =
            when {
                strongest.supportCount >=
                    HIGH_MIN_SUPPORT &&
                    agreement >=
                    HIGH_MIN_RATIO ->
                    PersonalLearningConfidence.HIGH

                strongest.supportCount >=
                    MEDIUM_MIN_SUPPORT &&
                    agreement >=
                    MEDIUM_MIN_RATIO ->
                    PersonalLearningConfidence.MEDIUM

                else ->
                    PersonalLearningConfidence.LOW
            }

        val action =
            when {
                confidence ==
                    PersonalLearningConfidence.HIGH &&
                    trustedForAutoApply ->
                    PersonalLearningAction.AUTO_APPLY

                confidence ==
                    PersonalLearningConfidence.HIGH ||
                    confidence ==
                    PersonalLearningConfidence.MEDIUM ->
                    PersonalLearningAction.SUGGEST

                else ->
                    PersonalLearningAction.ASK
            }

        val reason =
            when (action) {
                PersonalLearningAction.AUTO_APPLY ->
                    "Learned from " +
                        strongest.supportCount +
                        " of " +
                        total +
                        " reviewed transactions for this trusted identity."

                PersonalLearningAction.SUGGEST ->
                    "Suggested from " +
                        strongest.supportCount +
                        " of " +
                        total +
                        " reviewed transactions for this identity."

                PersonalLearningAction.ASK ->
                    "Only " +
                        strongest.supportCount +
                        " of " +
                        total +
                        " reviewed transactions support this category, so PesaTrack will ask instead of guessing."
            }

        return PersonalLearningSuggestion(
            identityId = identityId,
            categoryId =
                strongest.categoryId,
            direction = direction,
            confidence = confidence,
            action = action,
            supportCount =
                strongest.supportCount,
            totalReviewCount = total,
            reason = reason
        )
    }
}

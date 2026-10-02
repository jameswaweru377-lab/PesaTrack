package com.kedevjimmy.pesatrack.domain.learning

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection

enum class PersonalLearningConfidence {
    LOW,
    MEDIUM,
    HIGH
}

enum class PersonalLearningAction {
    ASK,
    SUGGEST,
    AUTO_APPLY
}

data class PersonalCategoryEvidence(
    val categoryId: String,
    val supportCount: Int
)

data class PersonalLearningSuggestion(
    val identityId: String,
    val categoryId: String,
    val direction: MoneyDirection,
    val confidence: PersonalLearningConfidence,
    val action: PersonalLearningAction,
    val supportCount: Int,
    val totalReviewCount: Int,
    val reason: String
)

data class PersonalLearningSnapshot(
    val reviewedTransactionCount: Int,
    val learnedIdentityCount: Int,
    val autoAppliedTransactionCount: Int
)

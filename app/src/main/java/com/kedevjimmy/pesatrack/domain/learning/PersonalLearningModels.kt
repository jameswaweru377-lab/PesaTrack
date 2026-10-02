package com.kedevjimmy.pesatrack.domain.learning

import com.kedevjimmy.pesatrack.domain.identity.IdentityAliasType
import com.kedevjimmy.pesatrack.domain.identity.IdentityEvidenceSource
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection

enum class PersonalLearningConfidence {
    HIGH,
    MEDIUM,
    LOW
}

enum class PersonalLearningAction {
    AUTO_APPLY,
    SUGGEST,
    ASK
}

data class PersonalLearningPreference(
    val identityId: String,
    val categoryId: String,
    val direction: MoneyDirection,
    val confirmationCount: Int,
    val sourceTransactionId: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

data class PersonalLearningSuggestion(
    val identityId: String,
    val identityLabel: String,
    val categoryId: String,
    val direction: MoneyDirection,
    val confidence: PersonalLearningConfidence,
    val action: PersonalLearningAction,
    val confirmationCount: Int,
    val matchedAliasType: IdentityAliasType?,
    val matchedAliasSource: IdentityEvidenceSource?
)

data class PersonalLearningApplication(
    val transactionId: Long,
    val identityId: String,
    val categoryId: String,
    val direction: MoneyDirection,
    val confidence: PersonalLearningConfidence,
    val confirmationCount: Int,
    val appliedAtEpochMs: Long
)

data class PersonalLearningExplanation(
    val application: PersonalLearningApplication,
    val identityLabel: String,
    val matchedAliasType: IdentityAliasType?,
    val matchedAliasSource: IdentityEvidenceSource?
)

enum class PersonalLearningCorrectionAction {
    UPDATE_PREFERENCE,
    FORGET_PREFERENCE
}

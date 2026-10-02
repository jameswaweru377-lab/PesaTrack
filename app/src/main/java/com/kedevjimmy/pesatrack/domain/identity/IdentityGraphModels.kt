package com.kedevjimmy.pesatrack.domain.identity

enum class IdentityNodeKind {
    SELF,
    PERSON,
    BUSINESS,
    BILLER,
    FINANCIAL_INSTITUTION,
    FINANCIAL_ACCOUNT,
    UNKNOWN
}

enum class IdentityOwnership {
    SELF,
    EXTERNAL,
    UNKNOWN
}

enum class IdentityConfidence {
    USER_CONFIRMED,
    HIGH,
    MEDIUM,
    LOW
}

enum class IdentityAliasType {
    NAME,
    PHONE,
    ACCOUNT_ID,
    ACCOUNT_IDENTIFIER,
    PROVIDER_KEY,
    SMS_SENDER
}

enum class IdentityEvidenceSource {
    USER_CONFIRMATION,
    ACCOUNT_STORE,
    FINANCIAL_ENTITY,
    TRANSACTION_COUNTERPARTY,
    LOCAL_ENTITY_CATALOG,
    REMOTE_ENRICHMENT,
    MESSAGE_SOURCE
}

enum class IdentityRelationType {
    OWNS,
    SAME_AS,
    TRANSACTED_WITH
}

data class IdentityNode(
    val identityId: String,
    val kind: IdentityNodeKind,
    val canonicalName: String,
    val ownership: IdentityOwnership,
    val confidence: IdentityConfidence,
    val userConfirmed: Boolean,
    val firstSeenAtEpochMs: Long,
    val lastSeenAtEpochMs: Long
)

data class IdentityAlias(
    val id: Long,
    val identityId: String,
    val type: IdentityAliasType,
    val normalizedValue: String,
    val displayValue: String,
    val source: IdentityEvidenceSource,
    val confidence: IdentityConfidence,
    val userConfirmed: Boolean,
    val firstSeenAtEpochMs: Long,
    val lastSeenAtEpochMs: Long,
    val occurrenceCount: Int
)

data class IdentityResolution(
    val node: IdentityNode,
    val matchedAlias: IdentityAlias?,
    val confidence: IdentityConfidence
)

enum class IdentityReviewPromptType {
    CONFIRM_SELF_PHONE,
    CONFIRM_SELF_NAME,
    CLASSIFY_COUNTERPARTY
}

data class IdentityReviewPrompt(
    val promptType: IdentityReviewPromptType,
    val identityId: String,
    val displayLabel: String,
    val aliasType: IdentityAliasType,
    val normalizedValue: String,
    val observationCount: Int,
    val reason: String
)



enum class IdentityReviewDecisionType {
    REJECTED_SELF,
    DEFERRED
}

data class IdentityReviewDecision(
    val promptType: IdentityReviewPromptType,
    val identityId: String,
    val decision: IdentityReviewDecisionType,
    val evidenceCount: Int,
    val updatedAtEpochMs: Long
)

data class IdentityGraphSnapshot(
    val nodeCount: Int,
    val aliasCount: Int,
    val selfAliasCount: Int,
    val pendingPromptCount: Int,
    val unclassifiedIdentityCount: Int
)

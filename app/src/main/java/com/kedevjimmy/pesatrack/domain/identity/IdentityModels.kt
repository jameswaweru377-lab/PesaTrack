package com.kedevjimmy.pesatrack.domain.identity

enum class IdentityKind {
    SELF,
    PERSON,
    BUSINESS,
    BILLER,
    FINANCIAL_INSTITUTION,
    ACCOUNT,
    UNKNOWN
}

enum class IdentityAliasType {
    NAME,
    PHONE,
    ACCOUNT_REFERENCE,
    PROVIDER_KEY,
    DOMAIN
}

enum class IdentityEvidenceSource {
    TRANSACTION_COUNTERPARTY,
    TRANSACTION_PHONE,
    ACCOUNT,
    PROVIDER,
    USER_CONFIRMATION,
    LOCAL_ENTITY,
    REMOTE_ENRICHMENT
}

enum class IdentityConfidence {
    OBSERVED,
    LIKELY,
    CONFIRMED
}

data class IdentityNode(
    val identityId: String,
    val kind: IdentityKind,
    val canonicalName: String,
    val userConfirmed: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

data class IdentityAlias(
    val aliasId: String,
    val identityId: String,
    val type: IdentityAliasType,
    val value: String,
    val normalizedValue: String,
    val source: IdentityEvidenceSource,
    val confidence: IdentityConfidence,
    val observationCount: Int,
    val firstSeenEpochMs: Long,
    val lastSeenEpochMs: Long
)

data class IdentityCandidate(
    val node: IdentityNode,
    val aliases: List<IdentityAlias>,
    val totalObservations: Int
)

data class IdentityObservation(
    val identityId: String,
    val canonicalName: String,
    val kind: IdentityKind,
    val matchedBy: IdentityAliasType
)

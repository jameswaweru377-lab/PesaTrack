package com.kedevjimmy.pesatrack.domain.statements

/** Read-only identity review. A mismatch is never proof that a document is fraudulent. */
object StatementBankIdentityReviewPolicy {
    data class Review(
        val matchedSourceIds: Set<String>,
        val mismatchedSourceIds: Set<String>,
        val unverifiableSourceIds: Set<String>
    ) {
        fun matches(sourceId: String): Boolean = sourceId in matchedSourceIds
        val needsReview: Boolean get() = mismatchedSourceIds.isNotEmpty() || unverifiableSourceIds.isNotEmpty()
    }

    /** Must use the same canonical full-number rule as the archive's SHA-256 identity lookup. */
    fun normalizeFullIdentifier(identifier: String?): String? {
        val normalized = identifier.orEmpty().uppercase(java.util.Locale.ROOT)
            .replace(Regex("[^A-Z0-9]"), "")
        return normalized.takeIf { it.length >= 8 && it.all(Char::isDigit) }
    }

    fun review(sourceIds: Set<String>, matchingIds: Set<String>, currentIdentifier: String?): Review {
        if (normalizeFullIdentifier(currentIdentifier) == null) {
            return Review(emptySet(), emptySet(), sourceIds)
        }
        val matched = sourceIds intersect matchingIds
        return Review(matched, sourceIds - matched, emptySet())
    }
}

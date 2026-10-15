package com.kedevjimmy.pesatrack.domain.everyday

import com.kedevjimmy.pesatrack.domain.identity.IdentityGraphPolicy
import com.kedevjimmy.pesatrack.domain.learning.RememberedCategoryMatchPolicy

/**
 * An explicitly remembered Activity category may resolve an independent PDF
 * row only when the complete extracted name agrees with ONE known identity.
 * PDFs often omit the phone number; unknown or competing identities are never
 * inferred from a substring. This policy makes no financial database writes.
 */
object StatementCategoryMemoryPolicy {
    data class Memory(val name: String?, val phone: String?, val categoryId: String)

    private fun exactName(left: String?, right: String?): Boolean =
        !left.isNullOrBlank() && !right.isNullOrBlank() &&
            RememberedCategoryMatchPolicy.matches(left, null, right, null)

    private fun identityKey(memory: Memory): String =
        IdentityGraphPolicy.normalizeKenyanPhone(memory.phone)
            ?: "NAME-ONLY"

    fun learnedFor(description: String, memories: List<Memory>): String? {
        val extracted = StatementFinancialPolicy.merchantKey(description) ?: return null
        val matched = memories.filter { exactName(it.name, extracted) }
        if (matched.isEmpty() || matched.map(::identityKey).distinct().size != 1) return null
        return matched.map { it.categoryId }.distinct().singleOrNull()
    }

    /**
     * Preview a NEW, user-approved Activity memory. Do not promise automatic
     * matches when another confirmed identity has the same displayed name but
     * a different verified phone number.
     */
    fun proposedSourceMatches(
        sourceName: String?,
        sourcePhone: String?,
        statementDescription: String,
        existingMemories: List<Memory>
    ): Boolean {
        val extracted = StatementFinancialPolicy.merchantKey(statementDescription) ?: return false
        if (!exactName(sourceName, extracted)) return false
        val proposed = Memory(sourceName, sourcePhone, "PROPOSED")
        val normalizedSourcePhone = identityKey(proposed)
        return existingMemories
            .filter { exactName(it.name, extracted) }
            .none { identityKey(it) != normalizedSourcePhone }
    }

    /**
     * Keep original PDF events and explicit one-off corrections untouched;
     * only candidate expense withdrawals with safe evidence can change.
     */
    fun wouldChange(
        batchId: String,
        line: StatementActivityLine,
        newCategory: String,
        currentCategory: String?,
        existingManualCorrection: Boolean,
        categoryKinds: Map<String, String>
    ): Boolean {
        if (existingManualCorrection ||
            !StatementFinancialPolicy.canAssignCategory(line, newCategory, categoryKinds)) return false
        val current = StatementFinancialPolicy.classify(
            batchId, line, rememberedCategory = currentCategory,
            categoryKinds = categoryKinds
        )
        return current.flow != StatementFlow.EXPENSE || current.categoryId != newCategory
    }
}

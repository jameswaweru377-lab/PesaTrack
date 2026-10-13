package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment

/** Shared semantic headings with local physical evidence; no provider names. */
data class StatementPdfColumn(
    val field: StatementField,
    val heading: List<PdfTextFragment>,
    val left: Float,
    val right: Float
)

data class StatementPdfColumns(val columns: List<StatementPdfColumn>) {
    /** Require the entire rectangle to belong to one column, not its centre. */
    fun owner(fragment: PdfTextFragment): StatementField? = columns.singleOrNull {
        fragment.left >= it.left && fragment.right <= it.right
    }?.field

    val originalHeadings: String get() = columns.joinToString(" ") { column ->
        column.heading.sortedWith(compareBy<PdfTextFragment> { it.top }.thenBy { it.left })
            .joinToString(" ") { it.text }
    }
}

object StatementPdfColumnDetector {
    /**
     * Caller supplies one observed header band. Split words and stacked labels
     * are merged only within this band. Unknown/extra headings are rejected;
     * extending a supported family requires an explicitly bounded profile.
     */
    fun detect(
        words: List<PdfTextFragment>,
        required: Set<StatementField>,
        profile: StatementFormatProfile = StatementFormatProfile()
    ): StatementPdfColumns? {
        if (words.size !in 3..80 || words.any {
            !it.left.isFinite() || !it.right.isFinite() || !it.top.isFinite() ||
                !it.bottom.isFinite() || it.left >= it.right || it.top >= it.bottom ||
                it.text.isBlank()
        } || words.maxOf { it.top } - words.minOf { it.top } > 24f) return null
        val ordered = words.sortedWith(compareBy<PdfTextFragment> { it.left }.thenBy { it.top })
        val headings = mutableListOf<Pair<StatementField, List<PdfTextFragment>>>()
        var index = 0
        while (index < ordered.size) {
            // Longest recognized phrase prevents "Transaction" and "Status"
            // or "Paid" and "In" from becoming independently guessed roles.
            val candidate = (minOf(4, ordered.size - index) downTo 1).firstNotNullOfOrNull { size ->
                val group = ordered.subList(index, index + size)
                // A stacked label must occupy one horizontal region. Never
                // steal "Transaction" from the next column to form the valid
                // but unrelated alias "Transaction Details".
                if (group.maxOf { it.top } - group.minOf { it.top } > 3f &&
                    group.maxOf { it.left } >= group.minOf { it.right }) return@firstNotNullOfOrNull null
                val value = group.sortedWith(compareBy<PdfTextFragment> { it.top }.thenBy { it.left })
                    .joinToString(" ") { it.text }
                val field = UniversalStatementTableInterpreter.fieldForHeader(value, profile)
                if (field in required) field to group.toList() else null
            } ?: return null
            headings.add(candidate)
            index += candidate.second.size
        }
        if (headings.map { it.first }.toSet() != required || headings.size != required.size)
            return null
        val bounds = headings.map { (_, boxes) -> boxes.minOf { it.left } to boxes.maxOf { it.right } }
        if (bounds.zipWithNext().any { (a, b) -> a.second >= b.first }) return null
        val boundaries = listOf(0f) + bounds.zipWithNext().map { (a, b) ->
            (a.second + b.first) / 2f
        } + Float.MAX_VALUE
        return StatementPdfColumns(headings.mapIndexed { i, (field, evidence) ->
            StatementPdfColumn(field, evidence, boundaries[i], boundaries[i + 1])
        })
    }
}

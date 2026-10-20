package com.kedevjimmy.pesatrack.domain.everyday

/** Converts only a whole-file-verified PDF's cash movements into review candidates. */
object StatementPdfActivityPolicy {
    fun toActivityRows(
        lines: List<StatementActivityLine>,
        proof: StatementActivityProof
    ): List<StatementRow> {
        require(proof.totalsVerified) { "The PDF has not passed complete financial verification." }
        require(lines.size == proof.receiptRows) { "The verified PDF row count changed." }
        require(lines.count { !it.informational } == proof.cashRows) {
            "The verified PDF cash-row count changed."
        }
        val rows = lines.filterNot { it.informational }.map { line ->
            StatementRow(
                date = line.occurredAt,
                description = line.description,
                signedMinor = line.movementMinor,
                reference = line.reference
            )
        }
        require(rows.isNotEmpty()) { "The verified PDF contains no cash movements." }
        StatementCsv.validateRows(rows)
        return rows
    }
}

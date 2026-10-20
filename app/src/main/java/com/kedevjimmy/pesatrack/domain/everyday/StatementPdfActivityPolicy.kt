package com.kedevjimmy.pesatrack.domain.everyday

import com.kedevjimmy.pesatrack.domain.model.AccountType

/** Converts only a whole-file-verified PDF's cash movements into review candidates. */
object StatementPdfActivityPolicy {
    fun toActivityRows(
        lines: List<StatementActivityLine>,
        proof: StatementActivityProof,
        accountType: AccountType
    ): List<StatementRow> {
        require(accountType == AccountType.MOBILE_WALLET) {
            "M-Pesa statement movements must use a mobile wallet account."
        }
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

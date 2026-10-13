package com.kedevjimmy.pesatrack.domain.everyday

import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment
import com.kedevjimmy.pesatrack.domain.statements.StatementField
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Raw evidence is transient, local and never included in diagnostic messages. */
data class PdfBoxReceiptEvidence(
    val page: Int,
    val row: Int,
    val reference: String,
    val dateText: String,
    val description: String,
    val cells: Map<StatementField, List<PdfTextFragment>>,
    val paidInMinor: Long,
    val withdrawnMinor: Long,
    val balanceMinor: Long
) {
    val movementMinor: Long get() = Math.subtractExact(paidInMinor, withdrawnMinor)
}

data class PdfBoxStatementProof(
    val receipts: List<PdfBoxReceiptEvidence>,
    val verifiedTransitions: Int,
    val totalsVerified: Boolean,
    val openingVerified: Boolean,
    val closingVerified: Boolean
)

/** Additional full-document gate for PDFBox only; legacy/CSV behavior is unchanged. */
object PdfBoxStatementFinancialAudit {
    private val summary = Regex(
        "(?im)^\\s*(TOTAL PAID IN|TOTAL WITHDRAWN|TOTAL PAID OUT|OPENING BALANCE|CLOSING BALANCE)" +
            "(?:\\s*\\((?:KES|KSH|KSHS)\\))?\\s*[:=]?\\s+(-?[0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*$"
    )

    fun verify(receipts: List<PdfBoxReceiptEvidence>, metadata: List<String>): PdfBoxStatementProof? {
        if (receipts.size !in 1..StatementCsv.MAX_ROWS) return null
        val zone = TimeZone.getTimeZone("Africa/Nairobi")
        val legs = receipts.map { evidence ->
            val pattern = if (evidence.dateText.contains('/')) "dd/MM/yyyy HH:mm:ss" else "yyyy-MM-dd HH:mm:ss"
            val pos = ParsePosition(0)
            val epoch = SimpleDateFormat(pattern, Locale.ROOT).apply {
                isLenient = false; timeZone = zone
            }.parse(evidence.dateText, pos)?.time ?: return null
            if (pos.index != evidence.dateText.length || epoch <= 0 || epoch > System.currentTimeMillis()) return null
            if (evidence.paidInMinor < 0 || evidence.withdrawnMinor < 0 || evidence.balanceMinor < 0 ||
                evidence.paidInMinor > 0 && evidence.withdrawnMinor > 0) return null
            StatementBalanceLeg(evidence.reference, epoch, evidence.movementMinor, evidence.balanceMinor)
        }
        // Duplicate identity includes description; a shared receipt alone is
        // never reason to discard a legitimate financial leg.
        val identities = receipts.mapIndexed { i, e ->
            listOf(e.reference.uppercase(Locale.ROOT), legs[i].occurredAt, e.movementMinor,
                e.description.trim().replace(Regex("\\s+"), " ").uppercase(Locale.ROOT))
        }
        if (identities.distinct().size != identities.size) return null
        val grouped = legs.groupBy { it.reference }
        if (grouped.values.any { group -> group.map { it.occurredAt }.distinct().size != 1 }) return null
        val declared = mutableMapOf<String, Long>()
        val summaryText=metadata.joinToString("\n")
        val labelled=Regex("(?im)^\\s*(?:TOTAL PAID IN|TOTAL WITHDRAWN|TOTAL PAID OUT|OPENING BALANCE|CLOSING BALANCE)\\b.*$")
        if(labelled.findAll(summaryText).any {summary.matchEntire(it.value)==null})return null
        for (match in summary.findAll(summaryText)) {
            val label = match.groupValues[1].uppercase(Locale.ROOT).replace("PAID OUT", "WITHDRAWN")
            val value = EverydayMoneyPolicy.money(match.groupValues[2]) ?: return null
            if (value == Long.MIN_VALUE) return null
            // Withdrawal totals may be printed negative; balances/credits cannot.
            val normalized = if (label == "TOTAL WITHDRAWN") kotlin.math.abs(value) else value
            if (normalized < 0 || declared[label]?.let { it != normalized } == true) return null
            declared[label] = normalized
        }
        fun sum(values: List<Long>) = runCatching { values.fold(0L, Math::addExact) }.getOrNull()
        val paid = sum(receipts.map { it.paidInMinor }) ?: return null
        val withdrawn = sum(receipts.map { it.withdrawnMinor }) ?: return null
        if (declared["TOTAL PAID IN"]?.let { it != paid } == true ||
            declared["TOTAL WITHDRAWN"]?.let { it != withdrawn } == true) return null
        val descending = legs.first().occurredAt >= legs.last().occurredAt
        val oldest = if (descending) legs.last().reference else legs.first().reference
        val newest = if (descending) legs.first().reference else legs.last().reference
        val opening = declared["OPENING BALANCE"]
        val closing = declared["CLOSING BALANCE"]
        var transitions = 0
        if (grouped.size > 1) {
            val audit = runCatching {
                StatementReceiptBalanceAudit.verify(legs, opening, closing)
            }.getOrNull() ?: return null
            // A 95% credible layout is insufficient for production PDFBox proof.
            if (!audit.allVerified || audit.unverifiedReferences.isNotEmpty()) return null
            transitions = audit.verifiedTransitions
        } else if (opening == null) return null
        if (opening != null) {
            val movement = sum(grouped.getValue(oldest).map { it.signedMinor }) ?: return null
            val expected = runCatching { Math.addExact(opening, movement) }.getOrNull() ?: return null
            if (grouped.getValue(oldest).none { it.balanceMinor == expected }) return null
        }
        if (closing != null && grouped.getValue(newest).none { it.balanceMinor == closing }) return null
        if (opening != null && closing != null) {
            val expected = runCatching { Math.subtractExact(Math.addExact(opening, paid), withdrawn) }.getOrNull()
            if (expected != closing) return null
        }
        return PdfBoxStatementProof(receipts.toList(), transitions,
            declared.containsKey("TOTAL PAID IN") && declared.containsKey("TOTAL WITHDRAWN"),
            opening != null, closing != null)
    }
}

package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import java.math.RoundingMode
import kotlin.math.abs

enum class EvidenceConfidence { HIGH, LIKELY }

data class FinancialMessageEvidence(
    val amountMinor: Long,
    val label: String,
    val phone: String?,
    val referenceCode: String?,
    val sourceLabel: String,
    val observedAtEpochMs: Long
)

data class TransactionEvidenceMatch(
    val label: String,
    val phone: String?,
    val sourceLabel: String,
    val confidence: EvidenceConfidence,
    val timeDeltaMs: Long
)

object TransactionEvidenceResolver {
    const val MAX_WINDOW_MS = 120_000L
    private const val HIGH_TIME_WINDOW_MS = 15_000L

    private val moneyRegex = Regex(
        """(?:Ksh|KES)\s*([0-9,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val financialCueRegex = Regex(
        """\b(?:account|bank|credited|credit|debited|debit|paid|payment|received|sent|transfer|transferred|transaction|deposit|withdraw|withdrawn|balance|m-pesa|mpesa)\b""",
        RegexOption.IGNORE_CASE
    )
    private val referenceRegex = Regex("""\b([A-Z0-9]{10})\b""", RegexOption.IGNORE_CASE)
    private val phoneRegex = Regex("""(?:\+?254|0)[17]\d{8}""")
    private val explicitLabelPatterns = listOf(
        Regex(
            """\b(?:to|from|at|merchant)\s+([A-Za-z0-9][A-Za-z0-9 &.'_-]{2,48}?)(?=\s+(?:on|at|for|ref|reference|balance|KES|Ksh)\b|[.,;]|$)""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """\b(?:beneficiary|recipient)\s*[:\-]\s*([A-Za-z0-9][A-Za-z0-9 &.'_-]{2,48}?)(?=[.,;]|$)""",
            RegexOption.IGNORE_CASE
        )
    )
    private val genericTitles = setOf(
        "messages", "message", "sms", "truecaller", "notification",
        "notifications", "android system", "system ui"
    )

    fun parseEvidence(
        title: String?,
        body: String,
        observedAtEpochMs: Long,
        sourceFallback: String? = null
    ): FinancialMessageEvidence? {
        val normalizedBody = body.replace(Regex("""\s+"""), " ").trim()
        if (!financialCueRegex.containsMatchIn(normalizedBody)) return null
        val amount = moneyRegex.find(normalizedBody)?.groupValues?.get(1)?.let(::amountToMinor)
            ?: return null
        val label = extractLabel(title, normalizedBody) ?: return null
        val sourceLabel = title?.trim()?.takeIf { it.isNotBlank() }
            ?: sourceFallback?.trim()?.takeIf { it.isNotBlank() }
            ?: "Nearby financial message"
        return FinancialMessageEvidence(
            amountMinor = amount,
            label = label,
            phone = phoneRegex.find(normalizedBody)?.value?.let(::normalizePhone),
            referenceCode = referenceRegex.find(normalizedBody)?.groupValues?.get(1)?.uppercase(),
            sourceLabel = sourceLabel,
            observedAtEpochMs = observedAtEpochMs
        )
    }

    fun chooseBest(
        parsed: ParsedMpesaTransaction,
        transactionObservedAtEpochMs: Long,
        evidence: Iterable<FinancialMessageEvidence>
    ): TransactionEvidenceMatch? =
        evidence.mapNotNull {
            match(parsed, transactionObservedAtEpochMs, it)
        }.sortedWith(
            compareByDescending<TransactionEvidenceMatch> {
                it.confidence == EvidenceConfidence.HIGH
            }.thenBy { it.timeDeltaMs }
        ).firstOrNull()

    fun match(
        parsed: ParsedMpesaTransaction,
        transactionObservedAtEpochMs: Long,
        evidence: FinancialMessageEvidence
    ): TransactionEvidenceMatch? {
        if (parsed.amountMinor <= 0L || parsed.amountMinor != evidence.amountMinor) return null
        val delta = abs(transactionObservedAtEpochMs - evidence.observedAtEpochMs)
        if (delta > MAX_WINDOW_MS) return null
        val sameReference = !parsed.transactionCode.isNullOrBlank() &&
            parsed.transactionCode.equals(evidence.referenceCode, ignoreCase = true)
        val samePhone = !parsed.counterpartyPhone.isNullOrBlank() &&
            parsed.counterpartyPhone == evidence.phone
        val confidence = if (sameReference || samePhone || delta <= HIGH_TIME_WINDOW_MS) {
            EvidenceConfidence.HIGH
        } else {
            EvidenceConfidence.LIKELY
        }
        return TransactionEvidenceMatch(
            label = evidence.label,
            phone = evidence.phone,
            sourceLabel = evidence.sourceLabel,
            confidence = confidence,
            timeDeltaMs = delta
        )
    }

    fun enrich(
        parsed: ParsedMpesaTransaction,
        match: TransactionEvidenceMatch?
    ): ParsedMpesaTransaction {
        if (match == null || match.confidence != EvidenceConfidence.HIGH ||
            !parsed.counterparty.isNullOrBlank()
        ) return parsed
        return parsed.copy(
            counterparty = match.label,
            counterpartyPhone = parsed.counterpartyPhone ?: match.phone
        )
    }

    private fun extractLabel(title: String?, body: String): String? {
        explicitLabelPatterns.forEach { pattern ->
            val value = pattern.find(body)?.groupValues?.get(1)?.cleanLabel()
            if (!value.isNullOrBlank()) return value
        }
        return title?.cleanLabel()?.takeIf {
            it.lowercase() !in genericTitles &&
                !it.equals("M-PESA", ignoreCase = true) &&
                it.length in 3..48
        }
    }

    private fun String.cleanLabel(): String =
        replace(phoneRegex, " ").replace(Regex("""\s+"""), " ")
            .trim(' ', '.', ',', ':', ';', '-')

    private fun normalizePhone(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        return when {
            digits.length == 12 && digits.startsWith("254") -> "+$digits"
            digits.length == 10 && digits.startsWith("0") -> "+254" + digits.drop(1)
            else -> digits
        }
    }

    private fun amountToMinor(raw: String): Long =
        raw.replace(",", "").toBigDecimal().movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP).longValueExact()
}

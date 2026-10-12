package com.kedevjimmy.pesatrack.domain.statements

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import com.kedevjimmy.pesatrack.domain.everyday.EverydayMoneyPolicy

/** A semantic role, never a physical column index or a provider-specific word. */
enum class StatementField {
    DATE, VALUE_DATE, DESCRIPTION, REFERENCE, STATUS,
    DEBIT, CREDIT, AMOUNT, DIRECTION, BALANCE, FEE, INFORMATION, UNKNOWN
}

data class StatementFormatProfile(
    val aliases: Map<String, StatementField> = emptyMap()
)

/**
 * Lossless ordered table input; a future PDF geometry adapter and structured
 * CSV adapter must build this only when no source row or column was discarded.
 * Each page can have a different column order.
 */
data class StatementTablePage(
    val pageNumber: Int,
    val headers: List<String>,
    val rows: List<List<String>>
)

/** Account-leg observation, NOT a final income/expense/transfer/debt label. */
data class StatementLegEvidence(
    val pageNumber: Int,
    val rowNumber: Int,
    val occurredAt: Long,
    val description: String,
    val reference: String?,
    val movementMinor: Long,
    val runningBalanceMinor: Long,
    val informationalZero: Boolean
)

enum class StatementBlockReason {
    EMPTY_DOCUMENT, INVALID_STRUCTURE, UNKNOWN_MONETARY_COLUMN, AMBIGUOUS_HEADER,
    UNSUPPORTED_FINANCIAL_COLUMN, INVALID_DATE, INVALID_AMOUNT, AMBIGUOUS_DIRECTION,
    UNKNOWN_STATUS, DUPLICATE_COMPLETE_ROW, INCONSISTENT_ORDER, INSUFFICIENT_BALANCE_PROOF,
    BALANCE_MISMATCH, TOO_MANY_ROWS
}

sealed class StatementTableResult {
    data class Accepted(
        val legs: List<StatementLegEvidence>,
        /** The oldest leg has no independently observed preceding balance. */
        val oldestLegUnanchored: Boolean,
        val sourceRows: Int
    ) : StatementTableResult()

    /** Metadata only: never includes names, amounts, identifiers or raw source text. */
    data class Blocked(
        val reason: StatementBlockReason,
        val pageNumber: Int? = null,
        val rowNumber: Int? = null
    ) : StatementTableResult()
}

/**
 * Read-only, provider-neutral table interpreter. Never persists financial data
 * and never assigns ledger semantics. Caller must separately prove table
 * extraction completeness, account ownership and transaction relationships.
 */
object UniversalStatementTableInterpreter {
    private const val MAX_ROWS = 5_000
    private const val MAX_COLUMNS = 40
    private val whitespace = Regex("\\s+")
    private val numeric = Regex("[+-]?(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\\.[0-9]{1,2})?")
    private val moneyLike = Regex("(?i)^(?:(?:KES|KSH|KSHS)\\s*)?[+-]?(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\\.[0-9]{1,2})?(?:\\s*(?:KES|KSH|KSHS))?$")
    private val currencyAlias = mapOf(
        "DATE" to StatementField.DATE,
        "TRANSACTION DATE" to StatementField.DATE,
        "POSTING DATE" to StatementField.DATE,
        "COMPLETION TIME" to StatementField.DATE,
        "DATE TIME" to StatementField.DATE,
        "VALUE DATE" to StatementField.VALUE_DATE,
        "DESCRIPTION" to StatementField.DESCRIPTION,
        "DETAILS" to StatementField.DESCRIPTION,
        "NARRATION" to StatementField.DESCRIPTION,
        "PARTICULARS" to StatementField.DESCRIPTION,
        "TRANSACTION DETAILS" to StatementField.DESCRIPTION,
        "REFERENCE" to StatementField.REFERENCE,
        "REF" to StatementField.REFERENCE,
        "RECEIPT NO" to StatementField.REFERENCE,
        "TRANSACTION ID" to StatementField.REFERENCE,
        "TRANSACTION REFERENCE" to StatementField.REFERENCE,
        "STATUS" to StatementField.STATUS,
        "TRANSACTION STATUS" to StatementField.STATUS,
        "DEBIT" to StatementField.DEBIT,
        "DEBITS" to StatementField.DEBIT,
        "DR" to StatementField.DEBIT,
        "WITHDRAWN" to StatementField.DEBIT,
        "WITHDRAWAL" to StatementField.DEBIT,
        "PAID OUT" to StatementField.DEBIT,
        "MONEY OUT" to StatementField.DEBIT,
        "CREDIT" to StatementField.CREDIT,
        "CREDITS" to StatementField.CREDIT,
        "CR" to StatementField.CREDIT,
        "PAID IN" to StatementField.CREDIT,
        "DEPOSIT" to StatementField.CREDIT,
        "MONEY IN" to StatementField.CREDIT,
        "AMOUNT" to StatementField.AMOUNT,
        "TRANSACTION AMOUNT" to StatementField.AMOUNT,
        "DR CR" to StatementField.DIRECTION,
        "DEBIT CREDIT" to StatementField.DIRECTION,
        "DIRECTION" to StatementField.DIRECTION,
        "TYPE DR CR" to StatementField.DIRECTION,
        "BALANCE" to StatementField.BALANCE,
        "RUNNING BALANCE" to StatementField.BALANCE,
        "RUNNING BAL" to StatementField.BALANCE,
        "CURRENT BAL" to StatementField.BALANCE,
        "CLOSING BALANCE" to StatementField.BALANCE,
        "FEE" to StatementField.FEE,
        "FEES" to StatementField.FEE,
        "CHARGES" to StatementField.FEE,
        "CHARGE" to StatementField.FEE,
        "BRANCH" to StatementField.INFORMATION,
        "CHANNEL" to StatementField.INFORMATION,
        "CURRENCY" to StatementField.INFORMATION
    )
    private val formats = listOf(
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
        "dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy HH:mm", "dd/MM/yyyy",
        "dd-MM-yyyy HH:mm:ss", "dd-MM-yyyy HH:mm", "dd-MM-yyyy"
    )
    private fun normalizedHeader(s: String): String =
        s.trim().uppercase(Locale.ROOT)
            .replace(Regex("[^A-Z0-9]+"), " ").replace(whitespace, " ").trim()
            .replace(Regex("(?:^| )(?:KES|KSH|KSHS)$"), "").trim()

    /** Safe header recognition reusable by independent geometry/CSV adapters. */
    fun fieldForHeader(header: String, profile: StatementFormatProfile = StatementFormatProfile()): StatementField {
        val key = normalizedHeader(header)
        val canonical = currencyAlias[key]
        if (canonical != null) return canonical
        return profile.aliases.entries.firstOrNull { normalizedHeader(it.key) == key }?.value
            ?: StatementField.UNKNOWN
    }

    private fun parseDate(input: String, zone: TimeZone, now: Long): Long? {
        val text = input.trim()
        for (pattern in formats) {
            val position = ParsePosition(0)
            val parsed = SimpleDateFormat(pattern, Locale.ROOT).apply {
                isLenient = false
                timeZone = zone
            }.parse(text, position)?.time
            if (parsed != null && parsed > 0 && parsed <= now && position.index == text.length)
                return parsed
        }
        return null
    }

    private fun amount(text: String): Long? =
        if (text.isBlank() || text.trim() == "-") 0L
        else EverydayMoneyPolicy.money(text.trim())

    private fun explicitZero(text: String): Boolean =
        numeric.matches(text.trim()) && EverydayMoneyPolicy.money(text.trim()) == 0L

    private fun direction(text: String): Int? = when (normalizedHeader(text)) {
        "DR", "DEBIT", "WITHDRAWAL", "OUT", "MONEY OUT" -> -1
        "CR", "CREDIT", "DEPOSIT", "IN", "MONEY IN" -> 1
        else -> null
    }

    fun interpret(
        pages: List<StatementTablePage>,
        zone: TimeZone = TimeZone.getTimeZone("Africa/Nairobi"),
        openingBalanceMinor: Long? = null,
        profile: StatementFormatProfile = StatementFormatProfile(),
        now: Long = System.currentTimeMillis()
    ): StatementTableResult {
        fun blocked(reason: StatementBlockReason, p: Int? = null, r: Int? = null) =
            StatementTableResult.Blocked(reason, p, r)
        if (pages.isEmpty()) return blocked(StatementBlockReason.EMPTY_DOCUMENT)
        val additionalAliases = profile.aliases.mapKeys { normalizedHeader(it.key) }
        if (additionalAliases.any { (key, role) ->
                key.isBlank() || role == StatementField.UNKNOWN ||
                    (currencyAlias[key] != null && currencyAlias[key] != role)
            }) return blocked(StatementBlockReason.AMBIGUOUS_HEADER)
        val aliases = currencyAlias + additionalAliases
        if (pages.size > 160 || pages.map { it.pageNumber }.distinct().size != pages.size ||
            pages.any { it.pageNumber <= 0 } || pages.zipWithNext().any { (a,b) -> b.pageNumber <= a.pageNumber })
            return blocked(StatementBlockReason.INVALID_STRUCTURE)
        val legs = mutableListOf<StatementLegEvidence>()
        val exactRows = hashSetOf<String>()
        var count = 0
        for (page in pages) {
            if (page.headers.isEmpty() || page.headers.size > MAX_COLUMNS ||
                page.headers.any { it.isBlank() } || page.rows.isEmpty())
                return blocked(StatementBlockReason.INVALID_STRUCTURE, page.pageNumber)
            val fields = page.headers.map { aliases[normalizedHeader(it)] ?: StatementField.UNKNOWN }
            val index = fields.indices.associateBy { fields[it] }
            if (fields.filter { it !in setOf(StatementField.UNKNOWN, StatementField.INFORMATION) }
                    .groupingBy { it }.eachCount().any { it.value > 1 })
                return blocked(StatementBlockReason.AMBIGUOUS_HEADER, page.pageNumber)
            if (listOf(StatementField.DATE, StatementField.DESCRIPTION, StatementField.BALANCE)
                    .any { it !in index } ||
                !((StatementField.CREDIT in index && StatementField.DEBIT in index) ||
                  StatementField.AMOUNT in index) ||
                (StatementField.AMOUNT in index &&
                    (StatementField.CREDIT in index || StatementField.DEBIT in index)))
                return blocked(StatementBlockReason.INVALID_STRUCTURE, page.pageNumber)
            fun get(row: List<String>, role: StatementField) =
                index[role]?.let { row[it].trim() }.orEmpty()
            page.rows.forEachIndexed { zeroIndex, row ->
                val rowNumber = zeroIndex + 1
                count++
                if (count > MAX_ROWS) return blocked(StatementBlockReason.TOO_MANY_ROWS, page.pageNumber, rowNumber)
                if (row.size != page.headers.size || row.all { it.isBlank() })
                    return blocked(StatementBlockReason.INVALID_STRUCTURE, page.pageNumber, rowNumber)
                for (i in fields.indices) {
                    when (fields[i]) {
                        StatementField.UNKNOWN ->
                            if (moneyLike.matches(row[i].trim()))
                                return blocked(StatementBlockReason.UNKNOWN_MONETARY_COLUMN, page.pageNumber, rowNumber)
                        StatementField.FEE ->
                            if (!row[i].isBlank() && row[i].trim() != "-" && !explicitZero(row[i]))
                                return blocked(StatementBlockReason.UNSUPPORTED_FINANCIAL_COLUMN, page.pageNumber, rowNumber)
                        else -> Unit
                    }
                }
                val epoch = parseDate(get(row, StatementField.DATE), zone, now)
                    ?: return blocked(StatementBlockReason.INVALID_DATE, page.pageNumber, rowNumber)
                val description = get(row, StatementField.DESCRIPTION)
                if (description.isBlank() || description.length > 200)
                    return blocked(StatementBlockReason.INVALID_STRUCTURE, page.pageNumber, rowNumber)
                val balanceText = get(row, StatementField.BALANCE)
                if (balanceText.isBlank() || balanceText == "-" || !numeric.matches(balanceText))
                    return blocked(StatementBlockReason.INVALID_AMOUNT, page.pageNumber, rowNumber)
                val balance = EverydayMoneyPolicy.money(balanceText)
                    ?: return blocked(StatementBlockReason.INVALID_AMOUNT, page.pageNumber, rowNumber)
                val status = get(row, StatementField.STATUS)
                if (StatementField.STATUS in index && normalizedHeader(status) !in
                    setOf("COMPLETED", "SUCCESS", "SUCCESSFUL", "POSTED"))
                    return blocked(StatementBlockReason.UNKNOWN_STATUS, page.pageNumber, rowNumber)
                val movement: Long
                if (StatementField.AMOUNT in index) {
                    val raw = get(row, StatementField.AMOUNT)
                    if (!numeric.matches(raw)) return blocked(StatementBlockReason.INVALID_AMOUNT, page.pageNumber, rowNumber)
                    val signed = EverydayMoneyPolicy.money(raw)
                        ?: return blocked(StatementBlockReason.INVALID_AMOUNT, page.pageNumber, rowNumber)
                    val sign = if (StatementField.DIRECTION in index)
                        direction(get(row, StatementField.DIRECTION)) ?: return blocked(
                            StatementBlockReason.AMBIGUOUS_DIRECTION, page.pageNumber, rowNumber)
                    else null
                    if (sign != null && signed < 0 && sign > 0)
                        return blocked(StatementBlockReason.AMBIGUOUS_DIRECTION, page.pageNumber, rowNumber)
                    if (sign == null) movement = signed
                    else {
                        val magnitude = if (signed < 0) Math.negateExact(signed) else signed
                        movement = if (sign < 0) -magnitude else magnitude
                    }
                } else {
                    val debitText = get(row, StatementField.DEBIT)
                    val creditText = get(row, StatementField.CREDIT)
                    val debit = amount(debitText)
                    val credit = amount(creditText)
                    if (debit == null || credit == null || credit < 0 || debit == Long.MIN_VALUE ||
                        (debit != 0L && credit != 0L))
                        return blocked(StatementBlockReason.INVALID_AMOUNT, page.pageNumber, rowNumber)
                    movement = if (debit == 0L) credit else -kotlin.math.abs(debit)
                    // Blank/placeholder cells do not prove a zero-value row.
                    if (movement == 0L && !(explicitZero(debitText) && explicitZero(creditText)))
                        return blocked(StatementBlockReason.INVALID_AMOUNT, page.pageNumber, rowNumber)
                }
                // A repeated receipt reference may legitimately refer to separate
                // financial legs. Only a completely identical row is rejected.
                val unique = listOf(
                    epoch.toString(), description, get(row, StatementField.REFERENCE),
                    movement.toString(), balance.toString()
                ).joinToString("|")
                if (!exactRows.add(unique))
                    return blocked(StatementBlockReason.DUPLICATE_COMPLETE_ROW, page.pageNumber, rowNumber)
                legs.add(StatementLegEvidence(
                    page.pageNumber, rowNumber, epoch, description,
                    get(row, StatementField.REFERENCE).ifBlank { null },
                    movement, balance, movement == 0L
                ))
            }
        }
        if (legs.isEmpty()) return blocked(StatementBlockReason.EMPTY_DOCUMENT)
        if (legs.size == 1 && openingBalanceMinor == null)
            return blocked(StatementBlockReason.INSUFFICIENT_BALANCE_PROOF)
        // Banks often print only a date, not a timestamp. Same-day rows are
        // ordered by the source table and proven using adjacent balances.
        val nonIncreasing = legs.zipWithNext().all { (a,b) -> a.occurredAt >= b.occurredAt }
        val nonDecreasing = legs.zipWithNext().all { (a,b) -> a.occurredAt <= b.occurredAt }
        if (!nonIncreasing && !nonDecreasing)
            return blocked(StatementBlockReason.INCONSISTENT_ORDER)
        val directions = listOf(true, false).filter { if (it) nonIncreasing else nonDecreasing }
        fun reconciles(descending: Boolean): Boolean {
            for (i in 0 until legs.size - 1) {
                val newer = if (descending) legs[i] else legs[i + 1]
                val older = if (descending) legs[i + 1] else legs[i]
                val expected = runCatching {
                    Math.addExact(older.runningBalanceMinor, newer.movementMinor)
                }.getOrNull()
                if (expected != newer.runningBalanceMinor) return false
            }
            if (openingBalanceMinor != null) {
                val oldest = if (descending) legs.last() else legs.first()
                val expected = runCatching {
                    Math.addExact(openingBalanceMinor, oldest.movementMinor)
                }.getOrNull()
                if (expected != oldest.runningBalanceMinor) return false
            }
            return true
        }
        val validDirections = directions.filter(::reconciles)
        if (validDirections.isEmpty()) return blocked(StatementBlockReason.BALANCE_MISMATCH)
        if (validDirections.size > 1 && legs.size > 1)
            return blocked(StatementBlockReason.INSUFFICIENT_BALANCE_PROOF)
        return StatementTableResult.Accepted(legs, openingBalanceMinor == null, count)
    }
}

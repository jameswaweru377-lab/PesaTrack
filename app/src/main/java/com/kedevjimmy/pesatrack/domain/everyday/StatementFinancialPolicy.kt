package com.kedevjimmy.pesatrack.domain.everyday

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Statement-only financial meaning. Nothing in this module writes to SMS or the debt ledger. */
enum class StatementFlow { EXPENSE, INCOME, TRANSFER, DEBT, SAVINGS, INVESTMENT, FEE, REFUND, INFORMATIONAL, UNKNOWN }
enum class StatementDebtAction { BORROWED, REPAID, CHARGE }

data class StatementClassified(
    val batchId: String,
    val line: StatementActivityLine,
    val flow: StatementFlow,
    val categoryId: String? = null,
    val evidence: String = "Unclassified",
    val debtProvider: String? = null,
    val debtAction: StatementDebtAction? = null
) {
    val amountMinor: Long get() = if (line.withdrawnMinor > 0L) line.withdrawnMinor else line.paidInMinor
}

data class StatementFinancialTotals(
    val expenseMinor: Long, val incomeMinor: Long, val transferMinor: Long,
    val debtMinor: Long, val savingMinor: Long, val feesMinor: Long,
    val unknownOutMinor: Long, val unknownInMinor: Long, val investmentMinor: Long,
    val refundsMinor: Long
)

object StatementFinancialPolicy {
    private val nairobi = TimeZone.getTimeZone("Africa/Nairobi")
    private val whitespace = Regex("\\s+")
    private val leading = listOf(
        Regex("(?i)^(?:customer\\s+transfer\\s+to|send\\s+money\\s+to|payment\\s+to|paid\\s+to|buy\\s+goods\\s+at|merchant\\s+payment\\s+to)\\s+"),
        Regex("(?i)^(?:received\\s+from|funds\\s+received\\s+from)\\s+")
    )
    private val lenders = listOf(
        "FULIZA" to Regex("(?i)\\bFULIZA\\b"),
        "M-Shwari" to Regex("(?i)\\bM[-\\s]?SHWARI\\b"),
        "KCB M-Pesa Loan" to Regex("(?i)\\bKCB[-\\s]?M[-\\s]?PESA\\s+LOAN\\b"),
        "Branch" to Regex("(?i)\\bBRANCH\\s+LOAN\\b"),
        "Tala" to Regex("(?i)\\bTALA\\s+LOAN\\b")
    )
    private val loanRepay = Regex("(?i)\\b(?:loan\\s+repay(?:ment)?|repay(?:ment)?\\s+(?:of\\s+)?loan|loan\\s+payment|pay\\s+(?:your\\s+)?outstanding)\\b")
    private val loanDraw = Regex("(?i)\\b(?:loan\\s+(?:disbursement|drawdown|advanced|credited)|borrowed|loan\\s+proceeds)\\b")
    private val loanFee = Regex("(?i)\\b(?:loan\\s+(?:fee|interest|penalty)|fuliza\\s+(?:fee|interest))\\b")
    private val saving = Regex("(?i)\\b(?:M[-\\s]?SHWARI\\s+LOCK|SAVINGS?\\s+(?:DEPOSIT|WITHDRAWAL|TRANSFER)|DEPOSIT\\s+TO\\s+SAVINGS?)\\b")
    private val transfer = Regex("(?i)\\b(?:OWN\\s+(?:ACCOUNT|WALLET)|SELF[-\\s]?TRANSFER|BANK[-\\s]?TO[-\\s]?M[-\\s]?PESA|M[-\\s]?PESA[-\\s]?TO[-\\s]?BANK|TRANSFER\\s+(?:TO|FROM)\\s+(?:EQUITY|KCB|CO[-\\s]?OP)\\s+(?:BANK|ACCOUNT))\\b")
    private val transferToPerson = Regex("(?i)^(?:customer\\s+transfer|send\\s+money|funds\\s+transfer|transfer\\s+to)\\b")
    private val fee = Regex("(?i)\\b(?:TRANSACTION\\s+(?:FEE|COST|CHARGE)|WITHDRAWAL\\s+CHARGE|SERVICE\\s+(?:FEE|CHARGE)|BANK\\s+CHARGE)\\b")
    private val refund = Regex("(?i)\\b(?:REFUND|REVERSAL|REVERSED)\\b")
    private val income = Regex("(?i)\\b(?:SALARY|PAYROLL|WAGES|DIVIDEND|BUSINESS\\s+INCOME)\\b")
    private val grocery = Regex("(?i)\\b(?:NAIVAS|QUICKMART|CARREFOUR|CHANDARANA)\\b")
    private val fuel = Regex("(?i)\\b(?:SHELL\\s+(?:SERVICE|PETROL|STATION)|TOTALENERGIES|RUBIS\\s+PETROL)\\b")
    private val utility = Regex("(?i)\\b(?:KPLC|KENYA\\s+POWER)\\b")
    private val restaurant = Regex("(?i)\\b(?:KFC|JAVA\\s+HOUSE|ARTCAFFE)\\b")
    private val merchant = Regex("(?i)\\b(?:BUY\\s+GOODS|MERCHANT\\s+PAYMENT|PAY\\s+BILL|PAYBILL|TILL\\s+(?:NO|NUMBER))\\b")
    private val investment = Regex("(?i)\\b(?:INVESTMENT\\s+(?:DEPOSIT|PURCHASE)|SHARE\\s+PURCHASE|STOCKBROKER)\\b")

    /**
     * Conservative matching. The entire merchant/recipient identity must match a
     * remembered rule; we never scan for a user's name somewhere in a long message.
     * Statement PDFs normally omit counterparty phone, so phone-only memories cannot
     * be matched without explicit review.
     */
    fun merchantKey(description: String): String? {
        var key = description.trim().replace(whitespace, " ")
        leading.firstNotNullOfOrNull { pattern ->
            pattern.find(key)?.takeIf { it.range.first == 0 }
        }?.let { key = key.substring(it.range.last + 1).trim() }
        if (key.length !in 3..90 || key.any { it.isDigit() } ||
            key.contains("ACCOUNT", true) || key.contains("FULIZA", true) ||
            key.contains("M-SHWARI", true) || key.contains("REPAYMENT", true) ||
            key.contains("TRANSFER FROM", true)) return null
        return key.uppercase(Locale.ROOT)
    }

    fun classify(
        batchId: String, line: StatementActivityLine,
        assignedCategory: String? = null, rememberedCategory: String? = null,
        categoryKinds: Map<String, String> = emptyMap()
    ): StatementClassified {
        fun result(flow: StatementFlow, category: String? = null, evidence: String,
                   provider: String? = null, action: StatementDebtAction? = null) =
            StatementClassified(batchId, line, flow, category, evidence, provider, action)
        if (line.informational) return result(StatementFlow.INFORMATIONAL, evidence = "Zero-value statement record")
        val description = line.description.trim()
        val provider = lenders.firstOrNull { it.second.containsMatchIn(description) }?.first
        // Preserve debt financial meaning before any generic merchant/category learning.
        if (provider != null) {
            when {
                line.withdrawnMinor > 0L && loanRepay.containsMatchIn(description) ->
                    return result(StatementFlow.DEBT, "DEBT_LOAN", "Explicit loan repayment", provider, StatementDebtAction.REPAID)
                line.paidInMinor > 0L && loanDraw.containsMatchIn(description) ->
                    return result(StatementFlow.DEBT, "DEBT_LOAN", "Explicit loan disbursement", provider, StatementDebtAction.BORROWED)
                line.withdrawnMinor > 0L && loanFee.containsMatchIn(description) ->
                    return result(StatementFlow.FEE, "FEES", "Explicit loan charge", provider, StatementDebtAction.CHARGE)
            }
            return result(StatementFlow.UNKNOWN, evidence = "Loan-related event needs review", provider = provider)
        }
        if (refund.containsMatchIn(description))
            return result(StatementFlow.REFUND, evidence = "Refund or reversal")
        if (saving.containsMatchIn(description))
            return result(StatementFlow.SAVINGS, "SAVINGS", "Savings movement")
        if (investment.containsMatchIn(description))
            return result(StatementFlow.INVESTMENT, "INVESTMENT", "Investment movement")
        if (transfer.containsMatchIn(description))
            return result(StatementFlow.TRANSFER, "TRANSFER", "Owned-account transfer evidence")
        if (fee.containsMatchIn(description) && line.withdrawnMinor > 0L)
            return result(StatementFlow.FEE, "FEES", "Explicit transaction charge")
        if (income.containsMatchIn(description) && line.paidInMinor > 0L)
            return result(StatementFlow.INCOME, "INCOME", "Income evidence")

        if (assignedCategory != null) {
            val kind = categoryKinds[assignedCategory]
            val flow = when {
                kind == "EXPENSE" && line.withdrawnMinor > 0L -> StatementFlow.EXPENSE
                kind == "INCOME" && line.paidInMinor > 0L -> StatementFlow.INCOME
                assignedCategory == "DEBT_LOAN" -> StatementFlow.DEBT
                assignedCategory == "SAVINGS" -> StatementFlow.SAVINGS
                assignedCategory == "INVESTMENT" -> StatementFlow.INVESTMENT
                kind == "TRANSFER" || kind == "ALLOCATION" -> StatementFlow.TRANSFER
                else -> StatementFlow.UNKNOWN
            }
            if (flow != StatementFlow.UNKNOWN)
                return result(flow, assignedCategory, "User category")
        }
        if (line.withdrawnMinor > 0L && rememberedCategory != null &&
            categoryKinds[rememberedCategory] == "EXPENSE")
            return result(StatementFlow.EXPENSE, rememberedCategory, "Remembered category")
        if (transferToPerson.containsMatchIn(description))
            return result(StatementFlow.TRANSFER, "TRANSFER", "Person-to-person transfer")
        if (line.withdrawnMinor > 0L) {
            val category = when {
                grocery.containsMatchIn(description) -> "GROCERIES"
                fuel.containsMatchIn(description) -> "TRANSPORT"
                utility.containsMatchIn(description) -> "ELECTRICITY"
                restaurant.containsMatchIn(description) -> "EATING_OUT"
                else -> null
            }
            if (category != null)
                return result(StatementFlow.EXPENSE, category, "Merchant suggestion")
            if (merchant.containsMatchIn(description))
                return result(StatementFlow.EXPENSE, "OTHER", "Merchant payment")
        }
        return result(StatementFlow.UNKNOWN, evidence = "Financial purpose not established")
    }

    fun totals(items: List<StatementClassified>): StatementFinancialTotals {
        val sum = { flow: StatementFlow ->
            items.asSequence().filter { it.flow == flow }
                .fold(0L) { acc, item -> Math.addExact(acc, item.amountMinor) }
        }
        return StatementFinancialTotals(
            expenseMinor = sum(StatementFlow.EXPENSE),
            incomeMinor = sum(StatementFlow.INCOME),
            transferMinor = sum(StatementFlow.TRANSFER),
            debtMinor = sum(StatementFlow.DEBT),
            savingMinor = sum(StatementFlow.SAVINGS),
            feesMinor = sum(StatementFlow.FEE),
            unknownOutMinor = items.filter { it.flow == StatementFlow.UNKNOWN }
                .fold(0L) { a, row -> Math.addExact(a, row.line.withdrawnMinor) },
            unknownInMinor = items.filter { it.flow == StatementFlow.UNKNOWN }
                .fold(0L) { a, row -> Math.addExact(a, row.line.paidInMinor) },
            investmentMinor = sum(StatementFlow.INVESTMENT),
            refundsMinor = sum(StatementFlow.REFUND)
        )
    }

    enum class Period { DAY, WEEK, MONTH, YEAR }
    fun within(time: Long, anchor: Long, period: Period): Boolean {
        val a = Calendar.getInstance(nairobi, Locale.ROOT).apply { timeInMillis = anchor }
        val row = Calendar.getInstance(nairobi, Locale.ROOT).apply { timeInMillis = time }
        if (period == Period.YEAR) return row.get(Calendar.YEAR) == a.get(Calendar.YEAR)
        if (period == Period.MONTH) return row.get(Calendar.YEAR) == a.get(Calendar.YEAR) &&
            row.get(Calendar.MONTH) == a.get(Calendar.MONTH)
        if (period == Period.DAY) return row.get(Calendar.YEAR) == a.get(Calendar.YEAR) &&
            row.get(Calendar.DAY_OF_YEAR) == a.get(Calendar.DAY_OF_YEAR)
        // ISO-style Monday week start, independent of device locale.
        a.set(Calendar.HOUR_OF_DAY, 0); a.set(Calendar.MINUTE, 0)
        a.set(Calendar.SECOND, 0); a.set(Calendar.MILLISECOND, 0)
        while (a.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) a.add(Calendar.DAY_OF_MONTH, -1)
        val start = a.timeInMillis
        a.add(Calendar.DAY_OF_MONTH, 7)
        return time >= start && time < a.timeInMillis
    }
}

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
    private val personPrefix = Regex(
        "(?i)^(?:customer\\s+transfer\\s+to|send\\s+money\\s+to|transfer\\s+to|payment\\s+to|paid\\s+to)\\s+"
    )
    private val optionalPhone = Regex(
        "^(?:\\+?254[17][0-9*]{8}|0[17][0-9*]{8}|[17][0-9*]{8})(?=\\s|\\s*-|[A-Za-z]|$)"
    )
    private val supportedName = Regex("[A-Z][A-Z'’&.-]*(?: [A-Z][A-Z'’&.-]*){1,7}")
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
    private val loanRepay = Regex("(?i)\\b(?:loan\\s+repay(?:ment)?|repay(?:ment)?\\s+(?:of\\s+)?loan|loan\\s+payment|pay\\s+(?:your\\s+)?outstanding|(?:fuliza|m[-\\s]?shwari)\\s+repay(?:ment)?)\\b")
    private val loanDraw = Regex("(?i)\\b(?:loan\\s+(?:disbursement|drawdown|advanced|credited)|borrowed|loan\\s+proceeds|(?:fuliza|m[-\\s]?shwari)\\s+(?:disbursement|drawdown))\\b")
    private val loanFee = Regex("(?i)\\b(?:loan\\s+(?:fee|interest|penalty)|fuliza\\s+(?:fee|interest))\\b")
    private val saving = Regex("(?i)\\b(?:M[-\\s]?SHWARI\\s+LOCK|SAVINGS?\\s+(?:DEPOSIT|WITHDRAWAL|TRANSFER)|DEPOSIT\\s+TO\\s+SAVINGS?)\\b")
    private val transfer = Regex("(?i)\\b(?:OWN\\s+(?:ACCOUNT|WALLET)|SELF[-\\s]?TRANSFER|BANK[-\\s]?TO[-\\s]?M[-\\s]?PESA|M[-\\s]?PESA[-\\s]?TO[-\\s]?BANK|TRANSFER\\s+(?:TO|FROM)\\s+(?:EQUITY|KCB|CO[-\\s]?OP)\\s+(?:BANK|ACCOUNT)|TRANSFER\\s+(?:TO|FROM)\\s+OWN\\s+(?:EQUITY|KCB|CO[-\\s]?OP)\\s+(?:BANK\\s+)?ACCOUNT)\\b")
    private val transferToPerson = Regex("(?i)^(?:customer\\s+transfer|send\\s+money|funds\\s+transfer|transfer\\s+to)\\b")
    private val fee = Regex("(?i)\\b(?:TRANSACTION\\s+(?:FEE|COST|CHARGE)|WITHDRAWAL\\s+CHARGE|SERVICE\\s+(?:FEE|CHARGE)|BANK\\s+CHARGE)\\b")
    private val refund = Regex("(?i)\\b(?:REFUND|REVERSAL|REVERSED)\\b")
    private val income = Regex("(?i)\\b(?:SALARY|PAYROLL|WAGES|DIVIDEND|BUSINESS\\s+INCOME)\\b")
    private val grocery = Regex("(?i)\\b(?:NAIVAS|QUICKMART|CARREFOUR|CHANDARANA)\\b")
    private val fuel = Regex("(?i)\\b(?:SHELL\\s+(?:SERVICE|PETROL|STATION)|TOTALENERGIES|RUBIS\\s+PETROL)\\b")
    private val utility = Regex("(?i)\\b(?:KPLC|KENYA\\s+POWER)\\b")
    private val restaurant = Regex("(?i)\\b(?:KFC|JAVA\\s+HOUSE|ARTCAFFE)\\b")
    // Generic Paybill is not evidence of consumption: it can represent a loan, savings or own transfer.
    private val merchant = Regex("(?i)\\b(?:BUY\\s+GOODS|MERCHANT\\s+PAYMENT|TILL\\s+(?:NO|NUMBER))\\b")
    private val safaricomBundle = Regex(
        "(?i)^(?:customer\\s+bundle\\s+purchase|recharge\\s+for\\s+customer)\\b"
    )
    // Safaricom may be concatenated directly to its merchant/till identifier.
    private val safaricomEvidence = Regex("(?i)(?:SAFARICOM|\\bDATA\\s+BUNDLES\\b)")
    private val investment = Regex("(?i)\\b(?:INVESTMENT\\s+(?:DEPOSIT|PURCHASE)|SHARE\\s+PURCHASE|STOCKBROKER)\\b")

    /**
     * Conservative matching. The entire merchant/recipient identity must match a
     * remembered rule; we never scan for a user's name somewhere in a long message.
     * Statement PDFs normally omit counterparty phone, so phone-only memories cannot
     * be matched without explicit review.
     */
    /**
     * Return a verified full Kenyan phone when the PDF explicitly places it
     * before the recipient name. Masked numbers deliberately return null.
     */
    fun recipientPhone(description: String): String? {
        val clean = description.trim().replace(whitespace, " ")
        val prefix = personPrefix.find(clean) ?: return null
        val rest = clean.substring(prefix.range.last + 1).trimStart(' ', '-')
        val rawPhone = optionalPhone.find(rest)?.value ?: return null
        return com.kedevjimmy.pesatrack.domain.identity.IdentityGraphPolicy
            .normalizeKenyanPhone(rawPhone)
    }

    fun merchantKey(description: String): String? {
        var key = description.trim().replace(whitespace, " ")
        val person = personPrefix.find(key)
        if (person != null) {
            // Verified statement descriptions may prefix the actual recipient
            // with a phone number and optional dash. Do not keep that ID in
            // the identity key, and NEVER match arbitrary text substrings.
            key = key.substring(person.range.last + 1).trimStart(' ', '-')
            optionalPhone.find(key)?.let { phone ->
                key = key.substring(phone.range.last + 1).trimStart(' ', '-')
            }
            // M-Pesa may append sender-side metadata after a named recipient.
            key = key.split(Regex("(?i)\\s+by\\s*-\\s*"), limit = 2).first().trim()
            val cleaned = key.uppercase(Locale.ROOT).replace(whitespace, " ")
            if (!supportedName.matches(cleaned) || cleaned.contains("ACCOUNT") ||
                cleaned.contains("FULIZA") || cleaned.contains("SHWARI") ||
                cleaned.contains("REPAYMENT") || cleaned.contains("BANK"))
                return null
            return cleaned
        }
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
        if (saving.containsMatchIn(description))
            return result(StatementFlow.SAVINGS, "SAVINGS", "Savings movement")
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
        if (line.withdrawnMinor > 0L &&
            safaricomBundle.containsMatchIn(description) &&
            safaricomEvidence.containsMatchIn(description))
            return result(StatementFlow.EXPENSE, "PHONE_AND_DATA", "Safaricom bundle purchase")
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

    /**
     * A manual category is valid only when it really changes this statement's
     * classification. Explicit debt/refund/owned-transfer/savings evidence
     * always wins. Do not offer debt classification without lender evidence.
     */
    fun canAssignCategory(
        line: StatementActivityLine, categoryId: String, categoryKinds: Map<String, String>
    ): Boolean {
        if (categoryId == "DEBT_LOAN") return false
        val kind = categoryKinds[categoryId]
        if (kind != "EXPENSE" && kind != "INCOME" &&
            kind != "TRANSFER" && kind != "ALLOCATION") return false
        val candidate = classify(
            batchId = "category-review", line = line,
            assignedCategory = categoryId, categoryKinds = categoryKinds
        )
        return candidate.evidence == "User category" && candidate.categoryId == categoryId
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

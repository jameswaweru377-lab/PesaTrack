package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.data.local.ActivitySummary
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivityDesign3MoneyTest {
    @Test fun moneyInExcludesAllOwnedTransferHopsAndUnreviewedReceipts() {
        val outsideReceipt = row(1, "INCOME", 10_000_00)
        val equityToWallet = row(2, "TRANSFER", 10_000_00)
        val walletToSecondBank = row(3, "TRANSFER", 10_000_00)
        val secondBankToWallet = row(4, "TRANSFER", 10_000_00)
        val walletToEquity = row(5, "TRANSFER", 10_000_00)
        val uncertainIncoming = row(6, "INCOME", 10_000_00)
        val all = listOf(outsideReceipt, equityToWallet, walletToSecondBank,
            secondBankToWallet, walletToEquity, uncertainIncoming)
        val trusted = all - uncertainIncoming
        val snapshot = ledger(trusted).copy(transactions = all)

        assertEquals(setOf(1L), activityTrustedIncomeIds(snapshot))
        assertEquals(6, snapshot.transactions.size)
    }

    @Test fun salaryReceivingBankIsNotRequiredForMoneyInClassification() {
        val externalBankReceipt = row(7, "INCOME", 52_000_00)
        assertEquals(setOf(7L), activityTrustedIncomeIds(ledger(listOf(externalBankReceipt))))
    }

    @Test fun moneyInFilterRejectsUnattributedCreditsEvenWhenLegacyDirectionWasIncome() {
        val recordedExternal = row(20, "INCOME", 3_000_00)
        val unknownBankCredit = row(21, "INCOME", 5_000_00)
            .copy(type = "DEPOSIT", counterparty = null)
        val ownedTransferReceived = row(22, "INCOME", 9_000_00)
            .copy(linkedAccountId = "owned-second-account")
        assertEquals(setOf(20L), activityTrustedIncomeIds(
            ledger(listOf(recordedExternal, unknownBankCredit, ownedTransferReceived))))
    }

    @Test fun moneyInAndPayCycleIncomeExcludeFullyRepaidPrincipal() {
        val allPrincipal = row(31, "INCOME", 2_000_00)
        val partialPrincipal = row(32, "INCOME", 3_000_00)
        val other = row(33, "INCOME", 5_000_00).copy(type = "DEPOSIT")
        val snapshot = ledger(listOf(allPrincipal, partialPrincipal, other)).copy(
            repaidReceivableMinorById = mapOf(31L to 2_000_00L, 32L to 1_000_00L))

        assertEquals(setOf(32L), activityTrustedIncomeIds(snapshot))
    }

    @Test fun investedFilterShowsOnlyConfirmedInvestmentPurchases() {
        val boughtShares = row(1, "EXPENSE", 2_500_00,
            category = "INVESTMENT", relation = TransactionStore.RELATION_INVESTMENT_BUY)
        val ordinaryExpense = row(2, "EXPENSE", 400_00, category = "INVESTMENT")
        val accountTransfer = row(3, "TRANSFER", 2_500_00, category = "INVESTMENT")
        val soldShares = row(4, "TRANSFER", 1_000_00,
            relation = TransactionStore.RELATION_INVESTMENT_SELL)

        assertEquals(listOf(boughtShares), listOf(
            boughtShares, ordinaryExpense, accountTransfer, soldShares
        ).filter(::isActivityConfirmedInvestmentBuy))
    }

    @Test fun fourCardsKeepInvestmentPurchasesSeparateFromOtherTransfers() {
        val ownedMove = row(1, "TRANSFER", 8_000_00)
        val purchasedShares = row(2, "EXPENSE", 2_500_00,
            category = "INVESTMENT", relation = TransactionStore.RELATION_INVESTMENT_BUY)
        val debtPrincipal = row(3, "TRANSFER", 700_00, category = "DEBT_LOAN")
        val investmentSale = row(4, "TRANSFER", 500_00,
            category = "INVESTMENT", relation = TransactionStore.RELATION_INVESTMENT_SELL)
        val snapshot = ledger(listOf(ownedMove, purchasedShares, debtPrincipal, investmentSale))

        val totals = activityDesign3Totals(snapshot)

        assertEquals(46_800_00L, totals.incomeMinor)
        assertEquals(13_240_00L, totals.expensesMinor)
        assertEquals(8_000_00L, totals.transfersMinor)
        assertEquals(2_500_00L, totals.investedMinor)
    }

    @Test fun dailyNetUsesTrustedCashLegsAndExcludesOwnTransfers() {
        val salary = row(1, "INCOME", 46_800_00, at = 1_700_000_000_000)
        val groceries = row(2, "EXPENSE", 1_950_00, at = salary.occurredAtEpochMs)
        val transfer = row(3, "TRANSFER", 8_000_00, at = salary.occurredAtEpochMs)
        val unreviewed = row(4, "INCOME", 60_000_00, at = salary.occurredAtEpochMs)
        val trusted = listOf(salary, groceries, transfer)

        assertEquals(44_850_00L, activityDesign3DailyNet(
            dayStart(salary.occurredAtEpochMs), trusted))
        assertEquals(0L, activityDesign3DailyNet(
            dayStart(salary.occurredAtEpochMs) - 24 * 60 * 60 * 1_000L, trusted))
        assertEquals(44_850_00L, activityDesign3DailyNet(
            dayStart(salary.occurredAtEpochMs), trusted + unreviewed - unreviewed))
    }

    @Test fun trustedRefundsIncreaseDailyNetWhileFeesDecreaseIt() {
        val payment = row(1, "EXPENSE", 700_00, fee = 20_00)
        val refund = row(2, "TRANSFER", 100_00, relation = TransactionStore.RELATION_REFUND)
        assertEquals(-620_00L, activityDesign3DailyNet(
            dayStart(payment.occurredAtEpochMs), listOf(payment, refund)))
    }

    @Test fun overflowWithholdsDailyNetAndInvestmentMetric() {
        val a = row(1, "INCOME", Long.MAX_VALUE)
        val b = row(2, "INCOME", 1L)
        assertNull(activityDesign3DailyNet(dayStart(a.occurredAtEpochMs), listOf(a, b)))
        val first = row(3, "EXPENSE", Long.MAX_VALUE,
            category = "INVESTMENT", relation = TransactionStore.RELATION_INVESTMENT_BUY)
        val second = row(4, "EXPENSE", 1L,
            category = "INVESTMENT", relation = TransactionStore.RELATION_INVESTMENT_BUY)
        assertNull(activityDesign3Totals(ledger(listOf(first, second))).investedMinor)
    }

    private fun ledger(trusted: List<StoredTransaction>) = ActivityLedgerSnapshot(
        transactions = trusted,
        summary = ActivitySummary(46_800_00, 15_740_00, 0, 11_700_00, 0, 0),
        trustedTransactions = trusted,
        confirmedOwnedAccountIds = setOf("owned-wallet")
    )

    private fun row(
        id: Long,
        direction: String,
        amount: Long,
        category: String = if (direction == "INCOME") "INCOME" else "OTHER",
        relation: String? = null,
        fee: Long = 0,
        at: Long = 1_700_000_000_000
    ) = StoredTransaction(
        id = id, transactionCode = "TEST$id", fingerprint = "synthetic-$id",
        type = "RECEIVED", amountMinor = amount, feeMinor = fee,
        balanceMinor = null, counterparty = "Fictional Client", counterpartyPhone = null,
        occurredAtText = null, occurredAtEpochMs = at, receivedAtEpochMs = at,
        sourcePackage = null, sourceKind = "historical_sms", direction = direction,
        category = category, reviewState = "AUTO_CLASSIFIED", ruleId = null,
        planningMonth = null, reversalOfCode = null, accountId = "owned-wallet",
        linkedAccountId = null, payCycleId = null, cycleCandidateReason = null,
        cycleOverride = null, relationType = relation
    )
}

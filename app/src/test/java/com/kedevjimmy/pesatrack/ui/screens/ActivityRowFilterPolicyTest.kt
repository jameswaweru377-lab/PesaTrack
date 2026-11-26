package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityRowFilterPolicyTest {
    @Test fun moneyInShowsOnlyTrustedExternalReceiptsNotCircularOwnTransfers() {
        val external = row(1, "INCOME", "AUTO_CLASSIFIED")
        val equityToWallet = row(2, "TRANSFER", "AUTO_CLASSIFIED")
        val walletToCoop = row(3, "TRANSFER", "AUTO_CLASSIFIED")
        val coopToWallet = row(4, "TRANSFER", "AUTO_CLASSIFIED")
        val walletToEquity = row(5, "TRANSFER", "AUTO_CLASSIFIED")
        val uncertain = row(6, "INCOME", "NEEDS_REVIEW")
        val records = listOf(external, equityToWallet, walletToCoop, coopToWallet, walletToEquity, uncertain)

        assertEquals(listOf(1L), filter(records, "Money In", setOf(1L)).map { it.id })
        assertEquals(listOf(6L), filter(records, "Needs Review", setOf(1L)).map { it.id })
        assertEquals(listOf(2L, 3L, 4L, 5L),
            filter(records, "Transfers", setOf(1L)).map { it.id })
    }

    @Test fun salaryIsNotRequiredAndNoBankIsAssumed() {
        val receiptIntoAnyBank = row(7, "INCOME", "AUTO_CLASSIFIED")
        assertEquals(listOf(7L),
            filter(listOf(receiptIntoAnyBank), "Money In", setOf(7L)).map { it.id })
    }

    @Test fun possibleDuplicatesAndExcludedRowsDoNotLeakIntoMoneyIn() {
        val valid = row(1, "INCOME", "AUTO_CLASSIFIED")
        val duplicate = row(2, "INCOME", "AUTO_CLASSIFIED")
        val excluded = row(3, "INCOME", "EXCLUDED")
        val records = listOf(valid, duplicate, excluded)
        assertEquals(listOf(1L), filter(records, "Money In", setOf(1L,2L,3L), setOf(2L)).map { it.id })
        assertEquals(listOf(2L), filter(records, "Needs Review", setOf(1L,2L,3L), setOf(2L)).map { it.id })
    }

    @Test fun unresolvedCreditsShowOnlyUnverifiedIncomeAndNeverDuplicates() {
        val unverified = row(11, "INCOME", "AUTO_CLASSIFIED")
        val needsReview = row(12, "INCOME", "NEEDS_REVIEW")
        val transfer = row(13, "TRANSFER", "AUTO_CLASSIFIED")
        val duplicate = row(14, "INCOME", "AUTO_CLASSIFIED")
        val excluded = row(15, "INCOME", "EXCLUDED")
        val records = listOf(unverified, needsReview, transfer, duplicate, excluded)
        assertEquals(listOf(11L, 12L),
            filter(records, "Unresolved Credits", emptySet(), setOf(14L)).map { it.id })
        assertEquals(emptyList<Long>(),
            filter(records, "Money In", emptySet(), setOf(14L)).map { it.id })
    }

    private fun filter(
        records: List<StoredTransaction>,
        filter: String,
        trusted: Set<Long>,
        duplicates: Set<Long> = emptySet()
    ) = activityFilteredRows(records, filter, "", duplicates, trusted, emptyMap()) { it }

    private fun row(id: Long, direction: String, review: String) = StoredTransaction(
        id = id, transactionCode = "FAKE$id", fingerprint = "synthetic-$id",
        type = "SYNTHETIC", amountMinor = 10_000_00, feeMinor = 0,
        balanceMinor = null, counterparty = null, counterpartyPhone = null,
        occurredAtText = null, occurredAtEpochMs = 1_700_000_000_000,
        receivedAtEpochMs = 1_700_000_000_000,
        sourcePackage = null, sourceKind = "TEST", direction = direction,
        category = "OTHER", reviewState = review, ruleId = null,
        planningMonth = null, reversalOfCode = null, accountId = null,
        linkedAccountId = null, payCycleId = null, cycleCandidateReason = null,
        cycleOverride = null, relationType = null
    )
}

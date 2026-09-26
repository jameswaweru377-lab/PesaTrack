package com.kedevjimmy.pesatrack.data.local

data class StoredTransaction(
    val id: Long,
    val transactionCode: String?,
    val fingerprint: String,
    val type: String,
    val amountMinor: Long,
    val feeMinor: Long,
    val balanceMinor: Long?,
    val counterparty: String?,
    val occurredAtText: String?,
    val receivedAtEpochMs: Long,
    val sourcePackage: String?,
    val sourceKind: String
)

data class LedgerSnapshot(
    val transactionCount: Int,
    val latestTransaction: StoredTransaction?,
    val latestBalanceMinor: Long?,
    val latestBalanceObservedAtEpochMs: Long?
)

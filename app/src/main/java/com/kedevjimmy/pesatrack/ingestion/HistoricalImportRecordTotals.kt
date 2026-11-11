package com.kedevjimmy.pesatrack.ingestion

/** Separates saved ledger movements from other interpreted financial evidence. */
internal data class HistoricalImportRecordTotals(
    val ledgerTransactions: Int = 0,
    val otherFinancialRecords: Int = 0
) {
    val allRecords: Int get() = ledgerTransactions + otherFinancialRecords

    fun plus(ledgerTransactions: Int, otherFinancialRecords: Int): HistoricalImportRecordTotals {
        require(ledgerTransactions >= 0 && otherFinancialRecords >= 0)
        return copy(
            ledgerTransactions = this.ledgerTransactions + ledgerTransactions,
            otherFinancialRecords = this.otherFinancialRecords + otherFinancialRecords
        )
    }
}

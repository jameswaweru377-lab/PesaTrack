package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.statements.StatementBankHistoryPolicy

/** Read-only adapter for a single confirmed bank account. No cross-account totals. */
class StatementBankReportReader(context: Context) {
    private val archive = StatementBankArchiveStore(context)

    fun monthsFor(accountId: String): List<StatementBankHistoryPolicy.Month> {
        val rows = archive.sourcesFor(accountId).flatMap { source ->
            archive.legsFor(source.id).map { leg ->
                StatementBankHistoryPolicy.SourceLeg(
                    sourceId = source.id, accountId = source.accountId,
                    providerKey = source.providerKey, importedAt = source.importedAt,
                    occurredAt = leg.occurredAt, movementMinor = leg.movementMinor,
                    categoryId = leg.categoryId
                )
            }
        }
        return StatementBankHistoryPolicy.selectForAccount(rows, accountId)
    }
}

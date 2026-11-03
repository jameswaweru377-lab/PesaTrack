package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.statements.StatementBankHistoryPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementBankIdentityReviewPolicy
import java.util.Calendar
import java.util.TimeZone

/** Read-only adapter for a single confirmed bank account. No cross-account totals. */
class StatementBankReportReader(context: Context) {
    private val archive = StatementBankArchiveStore(context)

    /** Historical sources remain accessible; differing/unknown full identities are flagged only. */
    fun identityReviewFor(
        accountId: String, currentIdentifier: String?, year: Int? = null
    ): StatementBankIdentityReviewPolicy.Review {
        val dates = year?.let { value ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Africa/Nairobi"))
            calendar.clear()
            calendar.set(value, Calendar.JANUARY, 1)
            val start = calendar.timeInMillis
            calendar.add(Calendar.YEAR, 1)
            start to calendar.timeInMillis
        }
        val sourceIds = archive.sourcesFor(accountId).asSequence()
            .filter { source -> dates == null ||
                (source.lastAt >= dates.first && source.firstAt < dates.second &&
                    archive.legsFor(source.id).any { it.occurredAt >= dates.first && it.occurredAt < dates.second })
            }
            .map { it.id }.toSet()
        val matchingIds = archive.sourceIdsMatchingIdentity(accountId, currentIdentifier)
        return StatementBankIdentityReviewPolicy.review(sourceIds, matchingIds, currentIdentifier)
    }

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

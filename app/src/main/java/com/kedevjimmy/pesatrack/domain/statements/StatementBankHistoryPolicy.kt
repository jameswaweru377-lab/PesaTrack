package com.kedevjimmy.pesatrack.domain.statements

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Explicitly source-specific bank cash movement: never infer spending or salary from narration. */
object StatementBankHistoryPolicy {
    data class SourceLeg(
        val sourceId: String, val accountId: String, val providerKey: String,
        val importedAt: Long, val occurredAt: Long, val movementMinor: Long,
        val categoryId: String? = null
    )

    data class Month(
        val key: String, val accountId: String, val providerKey: String,
        val sourceId: String, val alternativeSourceCount: Int,
        val rowCount: Int, val moneyInMinor: Long, val moneyOutMinor: Long,
        val classificationUnverified: Boolean = true
    ) {
        val containsOverlappingImports: Boolean get() = alternativeSourceCount > 0
        val netMinor: Long get() = Math.subtractExact(moneyInMinor, moneyOutMinor)
    }

    fun selectForAccount(legs: List<SourceLeg>, accountId: String): List<Month> {
        require(accountId.isNotBlank())
        require(legs.all { it.occurredAt > 0 && it.sourceId.isNotBlank() &&
            it.accountId.isNotBlank() && it.providerKey.isNotBlank() })
        val month = SimpleDateFormat("yyyy-MM", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("Africa/Nairobi")
        }
        return legs.filter { it.accountId == accountId }
            .groupBy { month.format(Date(it.occurredAt)) }
            .map { (key, entries) ->
                val alternatives = entries.groupBy { it.sourceId }
                val winning = alternatives.entries.maxWithOrNull(
                    compareBy<Map.Entry<String, List<SourceLeg>>> { it.value.size }
                        .thenBy { it.value.maxOf { row -> row.occurredAt } -
                            it.value.minOf { row -> row.occurredAt } }
                        .thenBy { it.value.maxOf { row -> row.importedAt } }
                        .thenBy { it.key }
                )!!
                val rows = winning.value
                require(rows.map { it.providerKey }.distinct().size == 1)
                val incoming = rows.filter { it.movementMinor > 0 }
                    .fold(0L) { sum, leg -> Math.addExact(sum, leg.movementMinor) }
                val outgoing = rows.filter { it.movementMinor < 0 }
                    .fold(0L) { sum, leg -> Math.addExact(sum, Math.negateExact(leg.movementMinor)) }
                Month(key, accountId, rows.first().providerKey, winning.key,
                    alternatives.size - 1, rows.size, incoming, outgoing)
            }
            .sortedByDescending { it.key }
    }
}

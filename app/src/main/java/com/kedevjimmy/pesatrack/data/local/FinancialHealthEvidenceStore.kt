package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import java.math.BigDecimal

/**
 * User-declared planning context, never evidence of an actual cash movement.
 * Uses the existing backed-up private PesaTrack preferences, not another DB.
 * Statements, transfers, investments and inferred balances cannot populate it.
 */
class FinancialHealthEvidenceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "pesatrack_preferences", Context.MODE_PRIVATE
    )

    data class Snapshot(
        val emergencyReserveMinor: Long?,
        val essentialMonthlyMinor: Long?,
        val requiredDebtPaymentsMinor: Long?,
        val lastConfirmedAtMs: Long?,
        val requiresRefresh: Boolean
    )

    fun snapshot(nowMs: Long = System.currentTimeMillis()): Snapshot {
        val recorded = prefs.getLong(KEY_TIME, 0L)
        val fresh = recorded > 0L && nowMs >= recorded &&
            (nowMs - recorded) <= FRESHNESS_MS
        return Snapshot(
            emergencyReserveMinor = if (fresh) read(KEY_RESERVE) else null,
            essentialMonthlyMinor = if (fresh) read(KEY_ESSENTIALS) else null,
            requiredDebtPaymentsMinor = if (fresh) read(KEY_DEBT) else null,
            lastConfirmedAtMs = recorded.takeIf { it > 0L },
            requiresRefresh = recorded > 0L && !fresh
        )
    }

    private fun read(key: String): Long? =
        if (!prefs.contains(key)) null else prefs.getLong(key, 0L)
            .takeIf { it in 0..MAX_MINOR }

    /** For editing expired settings; expiration never silently refreshes itself. */
    fun editableAmounts(): Triple<String, String, String> =
        Triple(read(KEY_RESERVE).asMoney(), read(KEY_ESSENTIALS).asMoney(),
            read(KEY_DEBT).asMoney())

    /**
     * Blank = unknown, explicit zero reserve is valid. All three inputs are
     * committed together. Reject invalid values without partial mutations.
     */
    fun save(
        emergencyReserveKes: String,
        essentialsKes: String,
        requiredDebtKes: String,
        nowMs: Long = System.currentTimeMillis()
    ) {
        require(nowMs > 0L)
        val reserve = parseMoney(emergencyReserveKes)
        val essentials = parseMoney(essentialsKes)
        val repayments = parseMoney(requiredDebtKes)
        require(reserve == null || (essentials != null && essentials > 0L)) {
            "Enter monthly essential costs to explain your savings buffer."
        }
        val editor = prefs.edit()
        listOf(
            KEY_RESERVE to reserve, KEY_ESSENTIALS to essentials,
            KEY_DEBT to repayments
        ).forEach { (key, value) ->
            if (value == null) editor.remove(key) else editor.putLong(key, value)
        }
        editor.putLong(KEY_TIME, nowMs)
        check(editor.commit()) { "Could not save your financial guidance details." }
    }

    private fun Long?.asMoney(): String =
        this?.let { BigDecimal.valueOf(it, 2).stripTrailingZeros().toPlainString() }
            ?: ""

    companion object {
        private const val KEY_TIME = "financial_health_v2_confirmed_at"
        private const val KEY_RESERVE = "financial_health_v2_emergency_minor"
        private const val KEY_ESSENTIALS = "financial_health_v2_essentials_minor"
        private const val KEY_DEBT = "financial_health_v2_required_debt_minor"
        private const val MAX_MINOR = 100_000_000_000_00L // KES 100 billion
        private const val FRESHNESS_MS = 30L * 24L * 60L * 60L * 1000L

        fun parseMoney(value: String): Long? {
            val trimmed = value.trim()
            if (trimmed.isEmpty()) return null
            require(trimmed.matches(Regex("[0-9]{1,12}(\\.[0-9]{1,2})?"))) {
                "Use a positive KES amount with up to two decimal places."
            }
            val minor = BigDecimal(trimmed).movePointRight(2).longValueExact()
            require(minor in 0..MAX_MINOR) { "Amount exceeds the supported range." }
            return minor
        }
    }
}

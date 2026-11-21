package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.ingestion.FinancialSmsSenderPolicy

/** User-approved extra SMS sender names. Never populated by automatic message scanning. */
class FinancialSmsSenderApprovalStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "pesatrack_financial_sms_senders", Context.MODE_PRIVATE
    )

    fun listApproved(): Set<String> = synchronized(LOCK) { listApprovedUnsafe() }

    /** Keep a scan's body read and an approval/revocation mutually exclusive. */
    fun <T> withApprovedSenders(block: (Set<String>) -> T): T =
        synchronized(LOCK) { block(listApprovedUnsafe()) }

    fun revision(): Long = synchronized(LOCK) { preferences.getLong(REVISION, 0L) }

    fun approve(sender: String): Boolean {
        val normalized = FinancialSmsSenderPolicy.normalize(sender)
        if (!FinancialSmsSenderPolicy.canApprove(sender) ||
            FinancialSmsSenderPolicy.isTrusted(sender)) return false
        return synchronized(LOCK) {
            val before = listApprovedUnsafe()
            if (normalized in before) return@synchronized true
            persist(before, before + normalized)
        }
    }

    fun remove(sender: String): Boolean {
        val normalized = FinancialSmsSenderPolicy.normalize(sender)
        if (normalized.isEmpty()) return false
        return synchronized(LOCK) {
            val before = listApprovedUnsafe()
            if (normalized !in before) return@synchronized false
            persist(before, before - normalized)
        }
    }

    private fun listApprovedUnsafe(): Set<String> =
        preferences.getStringSet(APPROVED, emptySet()).orEmpty().toSet()

    private fun persist(previous: Set<String>, updated: Set<String>): Boolean {
        val oldRevision = preferences.getLong(REVISION, 0L)
        val saved = preferences.edit()
            .putStringSet(APPROVED, updated)
            .putLong(REVISION, oldRevision + 1L)
            .commit()
        if (!saved) {
            // SharedPreferences can expose an in-memory update even if disk persistence fails.
            preferences.edit().putStringSet(APPROVED, previous)
                .putLong(REVISION, oldRevision).commit()
        }
        return saved
    }

    private companion object {
        const val APPROVED = "approved_financial_sender_ids"
        const val REVISION = "approved_financial_sender_revision"
        val LOCK = Any()
    }
}

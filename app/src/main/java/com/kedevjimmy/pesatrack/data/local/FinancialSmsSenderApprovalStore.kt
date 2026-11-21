package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.ingestion.FinancialSmsSenderPolicy

/** User-approved extra SMS sender names. Never populated by automatic message scanning. */
class FinancialSmsSenderApprovalStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "pesatrack_financial_sms_senders", Context.MODE_PRIVATE
    )

    fun listApproved(): Set<String> = preferences.getStringSet(APPROVED, emptySet())
        .orEmpty().toSet()

    fun approve(sender: String): Boolean {
        val normalized = FinancialSmsSenderPolicy.normalize(sender)
        if (!FinancialSmsSenderPolicy.canApprove(sender)) return false
        return preferences.edit()
            .putStringSet(APPROVED, listApproved() + normalized)
            .commit()
    }

    fun remove(sender: String): Boolean {
        val normalized = FinancialSmsSenderPolicy.normalize(sender)
        if (normalized.isEmpty()) return false
        return preferences.edit()
            .putStringSet(APPROVED, listApproved() - normalized)
            .commit()
    }

    private companion object {
        const val APPROVED = "approved_financial_sender_ids"
    }
}

package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import com.kedevjimmy.pesatrack.domain.rules.OwnedMirrorObservation
import com.kedevjimmy.pesatrack.domain.rules.OwnedMirrorTransfer
import com.kedevjimmy.pesatrack.domain.rules.OwnedMirrorTransferPolicy

internal object OwnedMirrorTransferReconciler {
    fun resolve(existing: StoredTransaction, direction: String, accountId: String?, linkedAccountId: String?,
                amountMinor: Long, references: Set<String>, existingReferences: Set<String>,
                accounts: List<MoneyAccount>, incomingHasRule: Boolean): OwnedMirrorTransfer? =
        OwnedMirrorTransferPolicy.resolve(
            OwnedMirrorObservation(existing.amountMinor, existing.direction, existing.accountId,
                existing.linkedAccountId, existingReferences,
                existing.ruleId != null || existing.reviewState in setOf("USER_CLASSIFIED", "EXCLUDED") ||
                    existing.type == "REVERSAL" || existing.category == "DEBT_LOAN" || existing.relationType != null),
            OwnedMirrorObservation(amountMinor, direction, accountId, linkedAccountId, references, incomingHasRule),
            accounts.filter { it.enabled }.map { it.id }.toSet()
        )

    fun apply(db: SQLiteDatabase, id: Long, transfer: OwnedMirrorTransfer, feeMinor: Long, accounts: List<MoneyAccount>) {
        val values = ContentValues().apply {
            put("direction", "TRANSFER")
            put("category", "TRANSFER")
            put("review_state", "AUTO_CLASSIFIED")
            put("account_id", transfer.sourceAccountId)
            put("linked_account_id", transfer.destinationAccountId)
            put("fee_minor", feeMinor)
            val source = accounts.firstOrNull { it.id == transfer.sourceAccountId }?.name
            val destination = accounts.firstOrNull { it.id == transfer.destinationAccountId }?.name
            if (source != null && destination != null) put("counterparty", "$source → $destination")
            put("evidence_source", "owned_mirror_transfer")
            // The legacy transaction balance is a wallet balance, not the bank's balance.
            if (transfer.sourceAccountId != AccountStore.DEFAULT_MPESA_ID) putNull("balance_minor")
            putNull("pay_cycle_id")
            putNull("cycle_candidate_reason")
        }
        db.update("transactions", values, "id = ?", arrayOf(id.toString()))
    }
}

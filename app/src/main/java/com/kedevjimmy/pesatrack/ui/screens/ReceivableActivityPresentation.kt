package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.*

data class ReceivableActivityLink(
    val kind: String, val personName: String, val principalMinor: Long, val active: Boolean,
    val sharedBill: Boolean = false, val sourceCategory: String? = null, val paymentMinor: Long? = null
) {
    val label: String get() = if (kind == PersonalReceivableStore.LENT) "Debts → Owed to me" else "Debts → Repayment received"
    val title: String get() = when {
        kind != PersonalReceivableStore.LENT -> "Repayment"
        sharedBill -> "Shared bill"
        else -> "Money lent"
    }
    val description: String get() = (if (sharedBill) "Shared bill with " else title + " • ") +
        personName + (sourceCategory?.let { " • " + it } ?: "") +
        " • " + label + " • " + receivableMoney(principalMinor) +
        if (!active) " • Source needs review" else ""
    val billSummary: String? get() = paymentMinor?.takeIf { sharedBill && active && it > principalMinor }
        ?.let { "Their share: " + receivableMoney(principalMinor) + " of " + receivableMoney(it) +
            " • Your share: " + receivableMoney(Math.subtractExact(it,principalMinor)) + " • Fees stay separate" }
}
/** Call on IO; rows do not issue independent database queries during composition. */
internal fun loadReceivableActivityLinks(context: Context): Map<Long,ReceivableActivityLink> = synchronized(CanonicalStoreLock.monitor) {
    val store = PersonalReceivableStore(context)
    val names = store.people().associate { it.id to it.name }
    val transactions = TransactionStore.get(context)
    val categories = CategoryStore.get(context)
    store.linkedEvents().mapValues { (_,event) ->
        val source = event.transactionId?.let(transactions::getById)
        ReceivableActivityLink(event.kind,names[event.personId] ?: "Person",event.amountMinor,event.active,
            event.sharedBill,source?.category?.let(categories::labelFor),source?.amountMinor)
    }
}

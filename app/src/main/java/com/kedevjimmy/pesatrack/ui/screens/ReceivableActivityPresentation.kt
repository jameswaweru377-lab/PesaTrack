package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import com.kedevjimmy.pesatrack.data.local.PersonalReceivableStore

data class ReceivableActivityLink(val kind: String, val personName: String, val principalMinor: Long, val active: Boolean) {
    val label: String get() = if (kind == PersonalReceivableStore.LENT) "Debts → Owed to me" else "Debts → Repayment received"
    val description: String get() = label + " • " + personName + " • " + receivableMoney(principalMinor) +
        if (!active) " • Source needs review" else ""
}
/** Call on IO; rows do not issue independent database queries during composition. */
internal fun loadReceivableActivityLinks(context: Context): Map<Long,ReceivableActivityLink> {
    val store = PersonalReceivableStore(context)
    val names = store.people().associate { it.id to it.name }
    return store.linkedEvents().mapValues { (_,event) ->
        ReceivableActivityLink(event.kind,names[event.personId] ?: "Person",event.amountMinor,event.active)
    }
}

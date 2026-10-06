package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.*

/** Save a target and rebuild its planning effects in one canonical database transaction. */
class PlanningRuleRepository(context:Context) {
    private val app=context.applicationContext
    fun save(name:String,payee:String,target:Long,bill:Boolean,existing:SmartRule?)=write {
        require(name.isNotBlank() && payee.isNotBlank() && target>0) {"Enter a name, receipt payee and a positive amount."}
        val unchanged=existing!=null && payee.trim()==existing.counterpartyExact.ifBlank {existing.counterpartyContains}
        RuleStore.get(app).save(name=name.trim(),counterpartyContains=if(unchanged)existing!!.counterpartyContains else "",counterpartyExact=if(unchanged)existing!!.counterpartyExact else payee.trim(),
            counterpartyPhoneEquals=if(unchanged)existing!!.counterpartyPhoneEquals else "",notificationTitleContains=if(unchanged)existing!!.notificationTitleContains else "",messageContains=if(unchanged)existing!!.messageContains else "",
            category=if(bill)existing?.category ?: TransactionCategory.OTHER else TransactionCategory.SAVINGS,direction=if(bill)MoneyDirection.EXPENSE else MoneyDirection.TRANSFER,
            planningMonthOffset=existing?.planningMonthOffset ?: 0,maximumAmountMinor=existing?.maximumAmountMinor,enabled=existing?.enabled ?: true,
            displayLabel=name.trim(),preset=if(bill)RulePreset.OTHER_RECURRING else RulePreset.SAVINGS,plannedAmountMinor=target,accountId=existing?.accountId,linkedAccountId=existing?.linkedAccountId,id=existing?.id ?: java.util.UUID.randomUUID().toString())
    }
    fun disable(id:String)=write {RuleStore.get(app).setEnabled(id,false)}
    private fun <T> write(block:()->T):T=synchronized(CanonicalStoreLock.monitor) {
        val db=PesaTrackDatabase.get(app).writableDatabase
        db.beginTransaction()
        val result=try {block().also {PayCycleStore.get(app).rebuild();db.setTransactionSuccessful()}}finally{db.endTransaction()}
        LedgerUpdateBus.notifyChanged()
        result
    }
}

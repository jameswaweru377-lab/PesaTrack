package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.everyday.*
import com.kedevjimmy.pesatrack.domain.model.*
import com.kedevjimmy.pesatrack.domain.rules.AccountDisplayPolicy
import java.util.UUID
import java.util.Calendar
import java.security.MessageDigest
import com.kedevjimmy.pesatrack.domain.evidence.*
import com.kedevjimmy.pesatrack.domain.ingestion.TransactionFingerprint

/** Separate allocations never create extra cash movements. Writes share the ledger lock. */
class EverydayMoneyStore(context:Context) {
 private val app=context.applicationContext
 private val database=PesaTrackDatabase.get(app)
 private val transactions=TransactionStore.get(app)
 fun ownedAccounts():List<MoneyAccount> = synchronized(CanonicalStoreLock.monitor) {
  val confirmed=FinancialUnderstandingStore.get(app).confirmedAccountIds()
  AccountStore.get(app).listAccounts().filter {AccountDisplayPolicy.isConfirmedOwned(it,confirmed) || it.id==AccountStore.DEFAULT_CASH_ID}
 }
 private fun requireOwned(id:String):MoneyAccount=ownedAccounts().firstOrNull {it.id==id} ?: error("Choose one of your accounts.")
 fun addEntry(token:String,accountId:String,amount:Long,date:Long,label:String,category:String,income:Boolean):Boolean = synchronized(CanonicalStoreLock.monitor) {
  require(token.isNotBlank())
  requireOwned(accountId);require(amount>0 && date>0 && date<=System.currentTimeMillis()) {"Check the amount and date."}
  require(label.trim().length in 1..200){"Add a short description."}
  val cat=CategoryStore.get(app).find(category) ?: error("Choose a category.")
  require(income || cat.budgetable){"Choose a spending category."}
  val kind=if(income)MoneyDirection.INCOME else MoneyDirection.EXPENSE
  FinancialEvidenceContract.requireCashCandidate(FinancialEvidenceRequest(
   FinancialEvidenceSource.MANUAL,if(income)FinancialEvidenceClaim.CASH_RECEIVED else FinancialEvidenceClaim.CASH_SENT,
   userConfirmed=true,ownAccountResolved=true
  ))
  val parsed=ParsedMpesaTransaction(null,MpesaTransactionType.OTHER,amount,label.trim(),feeMinor=0,balanceMinor=null,occurredAtText=null,rawMessage="manual-entry|$token",reversalOfCode=null)
  val fingerprint=TransactionFingerprint.of(parsed,date)
  val db=database.writableDatabase
  val alreadySaved=db.rawQuery("SELECT 1 FROM transactions WHERE fingerprint=? LIMIT 1",arrayOf(fingerprint)).use {it.moveToFirst()}
  if(alreadySaved)return@synchronized false
  db.beginTransaction()
  try {
   val added=transactions.insertIfNew(parsed,null,"manual_entry",System.currentTimeMillis(),date,rebuildPayCycles=false,runRelationshipReconciliation=false,runMirrorCorrelation=false,accountIdOverride=accountId,allowLegacyAccountFallback=false,directionOverride=kind,reviewStateOverride=ReviewState.USER_CLASSIFIED)
   if(added){
    val id=db.rawQuery("SELECT id FROM transactions WHERE fingerprint=?",arrayOf(fingerprint)).use {it.moveToFirst();it.getLong(0)}
    db.update("transactions",ContentValues().apply {put("category",cat.id)},"id = ?",arrayOf(id.toString()))
    EvidenceJournalStore.observe(db,"MANUAL",accountId,token,accountId,
      if(income)amount else -amount,date,null,id,"CREATED")
   }
   db.setTransactionSuccessful();added
  } finally {db.endTransaction();LedgerUpdateBus.notifyChanged()}
 }
 fun allocations(id:Long):List<MoneyPart> = synchronized(CanonicalStoreLock.monitor) {
  database.readableDatabase.rawQuery("SELECT category,amount_minor FROM transaction_allocations WHERE transaction_id = ? ORDER BY position",arrayOf(id.toString())).use {c->buildList {while(c.moveToNext())add(MoneyPart(c.getString(0),c.getLong(1)))}}
 }
 fun allAllocations():Map<Long,List<MoneyPart>> = synchronized(CanonicalStoreLock.monitor) {
  database.readableDatabase.rawQuery("SELECT transaction_id,category,amount_minor FROM transaction_allocations ORDER BY transaction_id,position",null).use {c->val out=linkedMapOf<Long,MutableList<MoneyPart>>();while(c.moveToNext())out.getOrPut(c.getLong(0)){mutableListOf()}.add(MoneyPart(c.getString(1),c.getLong(2)));out}
 }
 fun split(id:Long,parts:List<MoneyPart>) = synchronized(CanonicalStoreLock.monitor) {
  val row=transactions.getById(id) ?: error("Payment no longer exists.")
  require(row.direction==MoneyDirection.EXPENSE.name && row.reviewState!=ReviewState.NEEDS_REVIEW.name && row.reviewState!=ReviewState.EXCLUDED.name && row.relationType==null && row.reversalOfCode==null){"Confirm this expense before splitting it."}
  require(EverydayMoneyPolicy.validSplit(row.amountMinor,parts)){"Parts must add up to the payment, with different categories."}
  require(PersonalReceivableStore(app).eventForTransaction(id)==null){"Remove the personal loan link before splitting this payment into spending categories."}
  require(parts.all {CategoryStore.get(app).find(it.category)?.budgetable==true}){"Choose spending categories."}
  val db=database.writableDatabase;db.beginTransaction();try {
   db.delete("transaction_allocations","transaction_id = ?",arrayOf(id.toString()))
   parts.forEachIndexed {i,p->db.insertOrThrow("transaction_allocations",null,ContentValues().apply {put("transaction_id",id);put("position",i);put("category",p.category);put("amount_minor",p.minor)})}
   db.update("transactions",ContentValues().apply {put("review_state",ReviewState.USER_CLASSIFIED.name)},"id = ?",arrayOf(id.toString()))
   db.setTransactionSuccessful()
  }finally{db.endTransaction()};LedgerUpdateBus.notifyChanged()
 }
 fun removeSplit(id:Long)=synchronized(CanonicalStoreLock.monitor){database.writableDatabase.delete("transaction_allocations","transaction_id = ?",arrayOf(id.toString()));LedgerUpdateBus.notifyChanged()}
 data class Preview(val row:StatementRow,val duplicate:Boolean,val possibleMatch:Boolean=false)
 fun preview(accountId:String,rows:List<StatementRow>):List<Preview> = synchronized(CanonicalStoreLock.monitor) {
  requireOwned(accountId);StatementCsv.validateRows(rows)
  val existing=transactions.transactionsBetween(rows.minOfOrNull {it.date} ?: 0, (rows.maxOfOrNull {it.date} ?: 0)+2*86400000L)
  val seen=hashSetOf<String>()
  rows.map {r->
   val codeMatch=r.reference?.let {transactions.getByTransactionCode(it)}!=null
   val match=existing.any {e->e.accountId==accountId && e.amountMinor==kotlin.math.abs(r.signedMinor) && ((r.signedMinor>0 && e.direction==MoneyDirection.INCOME.name)||(r.signedMinor<0 && e.direction in setOf(MoneyDirection.EXPENSE.name,MoneyDirection.TRANSFER.name))) && sameDay(e.occurredAtEpochMs,r.date)}
   val key=importKey(accountId,r);val prior=database.readableDatabase.rawQuery("SELECT 1 FROM statement_import_entries WHERE import_key = ?",arrayOf(key)).use {it.moveToFirst()}
   Preview(r,prior||codeMatch||!seen.add(key),match)
  }
 }
 /** Entire selected import either commits or rolls back; recheck duplicates inside the lock. */
 fun importRows(accountId:String,rows:List<StatementRow>,expectedAccountType:AccountType?=null):Int = synchronized(CanonicalStoreLock.monitor) {
  val account=requireOwned(accountId)
  require(expectedAccountType==null || account.type==expectedAccountType){"The statement account changed. Choose the correct account and review again."}
  require(rows.isNotEmpty());StatementCsv.validateRows(rows)
  val db=database.writableDatabase;db.beginTransaction();var count=0
  try {
   preview(accountId,rows).forEach { item ->
    val r=item.row;val key=importKey(accountId,r)
    var target:Long?=null
    var linkReason="REFERENCE"
    if(!item.duplicate) {
     FinancialEvidenceContract.requireCashCandidate(FinancialEvidenceRequest(
      FinancialEvidenceSource.STATEMENT,if(r.signedMinor>0)FinancialEvidenceClaim.CASH_RECEIVED else FinancialEvidenceClaim.CASH_SENT,
      interpreted=true,userConfirmed=true,ownAccountResolved=true
     ))
     val p=ParsedMpesaTransaction(r.reference,MpesaTransactionType.OTHER,kotlin.math.abs(r.signedMinor),r.description,feeMinor=0,balanceMinor=null,occurredAtText=null,rawMessage="statement-entry|$key",reversalOfCode=null)
     val fingerprint=TransactionFingerprint.of(p,r.date)
     val added=transactions.insertIfNew(p,null,"statement_import",System.currentTimeMillis(),r.date,rebuildPayCycles=false,runRelationshipReconciliation=false,runMirrorCorrelation=false,accountIdOverride=accountId,allowLegacyAccountFallback=false,directionOverride=if(r.signedMinor>0)MoneyDirection.INCOME else MoneyDirection.EXPENSE,categoryOverride=TransactionCategory.UNCATEGORIZED,reviewStateOverride=ReviewState.NEEDS_REVIEW)
     if(added) {
      count++
      db.insertOrThrow("statement_import_entries",null,ContentValues().apply {put("import_key",key);put("account_id",accountId)})
      target=db.rawQuery("SELECT id FROM transactions WHERE fingerprint=?",arrayOf(fingerprint)).use {it.moveToFirst();it.getLong(0)}
      linkReason="CREATED"
     }
    }
    if(target==null) target=r.reference?.let {transactions.getByTransactionCode(it)?.id}
    EvidenceJournalStore.observe(db,"STATEMENT",accountId,key,accountId,r.signedMinor,r.date,r.reference,target,linkReason)
   };db.setTransactionSuccessful()
  }finally{db.endTransaction()};LedgerUpdateBus.notifyChanged();count
 }
 fun saveObservedBalance(accountId:String,balance:Long,expected:Long?):Unit=synchronized(CanonicalStoreLock.monitor) {
  val account=requireOwned(accountId);require(balance>=0 || account.type==AccountType.BANK){"Balance cannot be below zero for this account."}
  val now=System.currentTimeMillis();val current=PaydaySnapshotStore.get(app).currentAccountSnapshot(account,now).balanceMinor
  require(current==expected){"Your balance changed. Check it again before saving."}
  // A balance observation is not income, expense, or a balancing transaction.
  val entity=database.readableDatabase.rawQuery("SELECT entity_key FROM financial_entities WHERE confirmed_account_id = ? LIMIT 1",arrayOf(accountId)).use {if(it.moveToFirst())it.getString(0)else "user-account:$accountId"}
  database.writableDatabase.insertOrThrow("account_balance_observations",null,ContentValues().apply {put("observation_key","user-check:"+UUID.randomUUID());put("entity_key",entity);put("account_id",accountId);put("balance_minor",balance);put("confidence","HIGH");put("observed_at_epoch_ms",now);put("source_kind","user_balance_check");put("created_at_epoch_ms",now)})
  LedgerUpdateBus.notifyChanged()
 }
 private fun sameDay(a:Long,b:Long):Boolean {val x=Calendar.getInstance().apply {timeInMillis=a};val y=Calendar.getInstance().apply {timeInMillis=b};return x.get(Calendar.YEAR)==y.get(Calendar.YEAR)&&x.get(Calendar.DAY_OF_YEAR)==y.get(Calendar.DAY_OF_YEAR)}
 private fun importKey(account:String,r:StatementRow)=MessageDigest.getInstance("SHA-256").digest("$account|${r.date}|${r.signedMinor}|${r.description}|${r.reference.orEmpty()}".toByteArray()).joinToString(""){"%02x".format(it)}
}

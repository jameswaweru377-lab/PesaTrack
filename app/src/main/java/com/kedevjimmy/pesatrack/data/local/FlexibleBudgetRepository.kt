package com.kedevjimmy.pesatrack.data.local
import android.content.Context
import android.content.ContentValues
import com.kedevjimmy.pesatrack.domain.everyday.*

data class FlexibleBudgetOptions(val cadence:BudgetCadence=BudgetCadence.MONTHLY,val anchor:Int=1,val carryUnused:Boolean=false,val startsAt:Long=0)
data class FlexibleBudgetStatus(val options:FlexibleBudgetOptions,val window:BudgetWindow,val spent:Long,val carry:Long,val limit:Long)
class FlexibleBudgetRepository(context:Context) {
 private val app=context.applicationContext
 private val database=PesaTrackDatabase.get(app)
 fun options(category:String):FlexibleBudgetOptions=synchronized(CanonicalStoreLock.monitor){
  database.readableDatabase.rawQuery("SELECT cadence,anchor,carry_unused,starts_at_epoch_ms FROM flexible_budget_options WHERE category = ?",arrayOf(category)).use {c->if(!c.moveToFirst())FlexibleBudgetOptions()else FlexibleBudgetOptions(BudgetCadence.valueOf(c.getString(0)),c.getInt(1),c.getInt(2)!=0,c.getLong(3))}
 }
 fun save(category:String,cadence:BudgetCadence,anchor:Int,carry:Boolean)=synchronized(CanonicalStoreLock.monitor){
  require(CategoryStore.get(app).find(category)?.budgetable==true)
  BudgetPeriods.containing(System.currentTimeMillis(),cadence,anchor)
  val old=options(category)
  val reset=old.cadence!=cadence || old.anchor!=anchor || old.carryUnused!=carry
  database.writableDatabase.insertWithOnConflict("flexible_budget_options",null,ContentValues().apply {put("category",category);put("cadence",cadence.name);put("anchor",anchor);put("carry_unused",if(carry)1 else 0);put("starts_at_epoch_ms",if(reset||old.startsAt==0L)System.currentTimeMillis() else old.startsAt)},android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
  LedgerUpdateBus.notifyChanged()
 }
 fun delete(category:String)=synchronized(CanonicalStoreLock.monitor){database.writableDatabase.delete("flexible_budget_options","category = ?",arrayOf(category))}
 fun status(category:String,base:Long,now:Long=System.currentTimeMillis()):FlexibleBudgetStatus=synchronized(CanonicalStoreLock.monitor){
  val config=options(category);val current=BudgetPeriods.containing(now,config.cadence,config.anchor)
  val tx=TransactionStore.get(app);val categories=CategoryStore.get(app);val allocations=EverydayMoneyStore(app).allAllocations();val shares=PersonalReceivableStore(app).linkedShares()
  fun spent(start:Long,end:Long):Long=BudgetSpendingProjection.calculate(tx.activitySnapshotBetween(start,end,now,performMaintenance=false).trustedTransactions,tx::trustedRefundOriginal,categories::canonicalId,receivableLent={shares[it]?.lentMinor ?: 0L},allocations={allocations[it].orEmpty()})[category] ?: 0L
  var carry=0L
  if(config.carryUnused && config.startsAt>0 && config.startsAt<current.start){
   var period=BudgetPeriods.containing(config.startsAt,config.cadence,config.anchor);var steps=0
   while(period.end<=current.start){require(++steps<=2600){"This budget needs a new start date."};carry=BudgetPeriods.carryNext(base,carry,spent(period.start,period.end));period=BudgetPeriods.containing(period.end,config.cadence,config.anchor)}
  }
  FlexibleBudgetStatus(config,current,spent(current.start,current.end),carry,Math.addExact(base,carry))
 }
}

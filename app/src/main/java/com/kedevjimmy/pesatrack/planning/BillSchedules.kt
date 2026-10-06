package com.kedevjimmy.pesatrack.planning

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.kedevjimmy.pesatrack.MainActivity
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.SmartRule
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class ScheduledBill(val rule: SmartRule,val remainingMinor: Long,val dueDay: Int?,val reminders: Boolean)

/** Preferences describe reminders only. Payment status still comes from the canonical ledger. */
class BillSchedules(private val context:Context) {
    private val prefs=context.getSharedPreferences("bill_schedules",Context.MODE_PRIVATE)
    fun list(now:Long=System.currentTimeMillis()):List<ScheduledBill> {
        val trusted=TransactionStore.get(context).activitySnapshot(now).trustedTransactions
        return RuleStore.get(context).listRules(true).filter {it.direction==MoneyDirection.EXPENSE && (it.plannedAmountMinor ?: 0)>0}.map {rule ->
            val paid=trusted.filter {it.ruleId==rule.id && it.direction=="EXPENSE"}.sumOf {it.amountMinor}
            ScheduledBill(rule,((rule.plannedAmountMinor ?: 0)-paid).coerceAtLeast(0),prefs.getInt("day:${rule.id}",0).takeIf {it in 1..31},prefs.getBoolean("enabled:${rule.id}",false))
        }
    }
    fun save(ruleId:String,day:Int,enabled:Boolean) {
        require(day in 1..31)
        prefs.edit().putInt("day:$ruleId",day).putBoolean("enabled:$ruleId",enabled).apply()
        if(!enabled)(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(ruleId,921)
        schedule(context);LedgerUpdateBus.notifyChanged()
    }
    fun notifyDue() {
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(!manager.areNotificationsEnabled())return
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(NotificationChannel("bills","Bill reminders",NotificationManager.IMPORTANCE_DEFAULT))
        val now=Calendar.getInstance()
        val next=(now.clone() as Calendar).apply {add(Calendar.MONTH,1)}
        (list(now.timeInMillis).map {it to now}+list(next.timeInMillis).map {it to next}).forEach { (bill,month) ->
            if(!bill.reminders || bill.dueDay==null || bill.remainingMinor==0L)return@forEach
            val due=(month.clone() as Calendar).apply {set(Calendar.DAY_OF_MONTH,minOf(bill.dueDay,getActualMaximum(Calendar.DAY_OF_MONTH)));set(Calendar.HOUR_OF_DAY,23);set(Calendar.MINUTE,59);set(Calendar.SECOND,59)}
            val delta=due.timeInMillis-now.timeInMillis
            if(delta !in 0..TimeUnit.DAYS.toMillis(3))return@forEach
            val key="sent:${bill.rule.id}:${month.get(Calendar.YEAR)}:${month.get(Calendar.MONTH)}"
            if(prefs.getBoolean(key,false))return@forEach
            val open=PendingIntent.getActivity(context,921,Intent(context,MainActivity::class.java).putExtra(OPEN_BILLS,true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            manager.notify(bill.rule.id,921,NotificationCompat.Builder(context,"bills").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("${bill.rule.displayLabel ?: bill.rule.name} is coming up").setContentText("Open PesaTrack to check your bill plan.").setContentIntent(open).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build())
            prefs.edit().putBoolean(key,true).apply()
        }
    }
    companion object {
        const val OPEN_BILLS="open_bills"
        fun schedule(context:Context) {WorkManager.getInstance(context).enqueueUniquePeriodicWork("bill-reminders",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<BillReminderWorker>(12,TimeUnit.HOURS).build())}
    }
}
class BillReminderWorker(context:Context,params:WorkerParameters):Worker(context,params) {
    override fun doWork():Result=try {BillSchedules(applicationContext).notifyDue();Result.success()}catch(e:Exception){Result.retry()}
}

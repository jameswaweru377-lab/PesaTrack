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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.kedevjimmy.pesatrack.MainActivity
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.rules.PlanningReminderPolicy
import java.util.Calendar
import java.util.concurrent.TimeUnit
object PlanningReminders {
    const val OPEN_PLAN = "open_planning_reminder"
    private const val WORK = "planning-reminders"
    private fun prefs(c: Context) = c.getSharedPreferences("planning_reminders",0)
    fun enabled(c: Context, kind: String) = prefs(c).getBoolean(kind,false)
    fun setEnabled(c: Context, kind: String, enabled: Boolean) {
        prefs(c).edit().putBoolean(kind,enabled).putLong("enabled:$kind",System.currentTimeMillis()).apply()
        schedule(c)
        if(!enabled) {
            val manager = c.getSystemService(NotificationManager::class.java)
            manager.activeNotifications.filter { it.tag?.startsWith("$kind:") == true }.forEach { manager.cancel(it.tag, it.id) }
            manager.cancel(kind,925)
        }
    }
    fun schedule(c: Context) {
        if (!enabled(c,"budgets") && !enabled(c,"goals")) { WorkManager.getInstance(c).cancelUniqueWork(WORK); return }
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(WORK,ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequest.Builder(PlanningReminderWorker::class.java,6,TimeUnit.HOURS).build())
    }
    fun check(c: Context) {
        if (!enabled(c,"budgets") && !enabled(c,"goals")) return
        val now = Calendar.getInstance()
        if (now.get(Calendar.HOUR_OF_DAY) !in 8..20) return
        if (Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(c,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return
        if (!NotificationManagerCompat.from(c).areNotificationsEnabled()) return
        val prefs=prefs(c)
        val month="${now.get(Calendar.YEAR)}-${now.get(Calendar.MONTH)}"
        val progress=FinancialProgressRepository(c).read()
        if(enabled(c,"budgets")) progress.budgets.forEach { budget ->
            val level=PlanningReminderPolicy.budgetLevel(budget.saved,budget.target) ?: return@forEach
            post(c,"budgets", "budget:${budget.id}:$month:${budget.target}:$level",
                if(level=="over") "Budget limit reached" else "Getting close to your budget",
                "${budget.name}: you've used ${PlanningReminderPolicy.share(budget.saved,budget.target)} of this month's budget.","budgets")
        }
        if(enabled(c,"goals")) {
            val store=TransactionStore.get(c)
            val rows=store.activitySnapshotBetween(0,now.timeInMillis+1,now.timeInMillis,performMaintenance=false).trustedTransactions
            val types=AccountStore.get(c).listAccounts(false).associate { it.id to it.type }
            val baseline=prefs.getLong("enabled:goals",now.timeInMillis)
            progress.goals.forEach { goal ->
                val last=GoalSavingHistoryPolicy.observations(rows,goal.id,types).filter { it.deltaMinor>0 }.maxOfOrNull { it.epochMs } ?: baseline
                val due=Calendar.getInstance().apply { timeInMillis=last; add(Calendar.MONTH,1) }.timeInMillis
                if(PlanningReminderPolicy.goalDue(now.timeInMillis,due,goal.saved,goal.target))
                    post(c,"goals","goal:${goal.id}:$month:$last","Keep your savings moving","It's been a month since a contribution to ${goal.name}. Save a little when you can.","saving-goals")
            }
        }
    }
    private fun post(c: Context, kind: String, key: String, title: String, text: String, route: String) {
        val prefs=prefs(c)
        if (!enabled(c,kind) || prefs.getBoolean(key,false)) return
        val manager=c.getSystemService(NotificationManager::class.java)
        if(Build.VERSION.SDK_INT>=26) manager.createNotificationChannel(NotificationChannel("planning","Budget and savings reminders",NotificationManager.IMPORTANCE_DEFAULT))
        val intent=Intent(c,MainActivity::class.java).putExtra(OPEN_PLAN,route).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val open=PendingIntent.getActivity(c,if(kind=="budgets") 925 else 926,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val public=NotificationCompat.Builder(c,"planning").setSmallIcon(R.drawable.ic_pesatrack_logo).setContentTitle("PesaTrack reminder").setContentText("Open PesaTrack to see your reminder.").build()
        val notification=NotificationCompat.Builder(c,"planning").setSmallIcon(R.drawable.ic_pesatrack_logo).setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(open).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(public).build()
        manager.notify("$kind:$key",925,notification)
        prefs.edit().putBoolean(key,true).apply()
    }
}
class PlanningReminderWorker(c: Context, p: WorkerParameters):Worker(c,p) {
    override fun doWork():Result=try { PlanningReminders.check(applicationContext); Result.success() } catch (_: Exception) { Result.retry() }
}

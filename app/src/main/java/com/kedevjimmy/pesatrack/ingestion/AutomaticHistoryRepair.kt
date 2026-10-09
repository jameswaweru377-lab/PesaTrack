package com.kedevjimmy.pesatrack.ingestion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import kotlin.math.max

object AutomaticHistoryRepairPolicy {
    private const val MAX_HISTORY_MS = 100L * 24L * 60L * 60L * 1_000L

    // Never widen a previous import. Old installations use only their captured SMS range.
    fun startEpochMs(importedSince: Long?, earliestCapturedSms: Long?, now: Long): Long? {
        val authorizedStart = importedSince ?: earliestCapturedSms ?: return null
        if (authorizedStart <= 0L || authorizedStart > now) return null
        return max(authorizedStart, now - MAX_HISTORY_MS)
    }
}

/** Local, once-per-update repair. No permission request, network, or ledger reset. */
object AutomaticHistoryRepair {
    private const val PREFS = "automatic_history_repair"
    private const val IMPORTED_SINCE = "imported_since"
    private const val REPAIRED_UPDATE = "repaired_update"

    fun recordImport(context: Context, sinceEpochMs: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(IMPORTED_SINCE, sinceEpochMs).apply()
    }

    fun scheduleIfAllowed(context: Context) {
        if (!context.getSharedPreferences("pesatrack_preferences", Context.MODE_PRIVATE)
                .getBoolean("setup_complete", false) || !hasSmsAccess(context)) return
        val update = updateEpochMs(context)
        if (update <= repairedUpdate(context)) return
        WorkManager.getInstance(context).enqueueUniqueWork(
            "pesatrack-automatic-history-repair", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequest.Builder(AutomaticHistoryRepairWorker::class.java).build()
        )
    }

    internal fun hasSmsAccess(context: Context): Boolean =
        com.kedevjimmy.pesatrack.data.local.SmsAccessConsentStore.canRead(context)

    internal fun updateEpochMs(context: Context): Long = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
    }.getOrDefault(0L)

    internal fun repairedUpdate(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(REPAIRED_UPDATE, 0L)

    internal fun startEpochMs(context: Context, now: Long): Long? {
        val importedSince = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(IMPORTED_SINCE, 0L).takeIf { it > 0L }
        val earliestSms = if (importedSince == null) {
            PesaTrackDatabase.get(context).readableDatabase.rawQuery(
                "SELECT MIN(received_at_epoch_ms) FROM transactions WHERE source_kind IN (?, ?) OR source_kind LIKE ?",
                arrayOf(HistoricalSmsImporter.SOURCE_HISTORICAL_SMS,
                    HistoricalSmsImporter.SOURCE_HISTORICAL_FINANCIAL_MESSAGE, "interpreted_historical%")
            ).use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
            }
        } else null
        return AutomaticHistoryRepairPolicy.startEpochMs(importedSince, earliestSms, now)
    }

    internal fun recordSuccess(context: Context, update: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(REPAIRED_UPDATE, update).apply()
    }
}

class AutomaticHistoryRepairWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result {
        if (!AutomaticHistoryRepair.hasSmsAccess(applicationContext)) return Result.success()
        val update = AutomaticHistoryRepair.updateEpochMs(applicationContext)
        if (update <= AutomaticHistoryRepair.repairedUpdate(applicationContext)) return Result.success()
        return try {
            val since = AutomaticHistoryRepair.startEpochMs(applicationContext, System.currentTimeMillis())
                ?: return Result.success()
            HistoricalSmsImporter(applicationContext).importSince(
                sinceEpochMs = since, mode = HistoricalImportMode.REPROCESS
            )
            AutomaticHistoryRepair.recordSuccess(applicationContext, update)
            Result.success()
        } catch (_: SecurityException) {
            // Access can be revoked between scheduling and the SMS query.
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }
}

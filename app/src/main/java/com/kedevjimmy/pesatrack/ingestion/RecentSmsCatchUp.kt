package com.kedevjimmy.pesatrack.ingestion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.PayCycleStore
import com.kedevjimmy.pesatrack.data.local.SmsAccessConsentStore
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

/**
 * Lightweight foreground safety net for phones that delay or suspend
 * notification-listener delivery while PesaTrack is in the background.
 *
 * Live notifications remain the primary path. When the app returns to the
 * foreground and READ_SMS is available, this scans only the small recent
 * window since the previous successful catch-up. Historical ingestion is
 * idempotent, so notification-captured messages are ignored as duplicates.
 */
object RecentSmsCatchUp {
    private const val PREFS =
        "pesatrack_runtime_sync"
    private const val LAST_ATTEMPT =
        "recent_sms_last_attempt"
    private const val LAST_SUCCESS =
        "recent_sms_last_success"
    private const val MIN_ATTEMPT_GAP_MS =
        60_000L
    private const val OVERLAP_MS =
        5L * 60L * 1000L
    private const val FIRST_WINDOW_MS =
        2L * 24L * 60L * 60L * 1000L
    private const val START_DELAY_MS =
        700L

    private val running =
        AtomicBoolean(false)
    private val executor =
        Executors
            .newSingleThreadScheduledExecutor()

    fun schedule(context: Context) {
        val appContext =
            context.applicationContext
        if (!SmsAccessConsentStore.canRead(appContext)) return
        if (
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.READ_SMS
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val preferences =
            appContext.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
        val now =
            System.currentTimeMillis()
        val lastAttempt =
            preferences.getLong(
                LAST_ATTEMPT,
                0L
            )
        if (
            now - lastAttempt <
            MIN_ATTEMPT_GAP_MS
        ) {
            return
        }
        if (!running.compareAndSet(false, true)) {
            return
        }

        preferences.edit()
            .putLong(
                LAST_ATTEMPT,
                now
            )
            .apply()

        executor.schedule(
            {
                var changed = false
                LedgerUpdateBus
                    .processingStarted()
                try {
                    if (!SmsAccessConsentStore.canRead(appContext)) return@schedule
                    if (
                        ContextCompat
                            .checkSelfPermission(
                                appContext,
                                Manifest.permission
                                    .READ_SMS
                            ) !=
                        PackageManager
                            .PERMISSION_GRANTED
                    ) {
                        return@schedule
                    }

                    val syncNow =
                        System.currentTimeMillis()
                    val lastSuccess =
                        preferences.getLong(
                            LAST_SUCCESS,
                            0L
                        )
                    val oldestAllowed =
                        syncNow -
                            FIRST_WINDOW_MS
                    val since =
                        if (lastSuccess > 0L) {
                            max(
                                oldestAllowed,
                                lastSuccess -
                                    OVERLAP_MS
                            )
                        } else {
                            oldestAllowed
                        }

                    val result =
                        HistoricalSmsImporter(
                            appContext
                        ).importSince(
                            sinceEpochMs = since,
                            rebuildPayCycles =
                                false
                        )

                    changed =
                        result.inserted > 0 ||
                            result.stateRefreshed >
                                0 ||
                            result.pendingReview >
                                0

                    if (result.inserted > 0) {
                        PayCycleStore.get(
                            appContext
                        ).rebuild()
                    }

                    preferences.edit()
                        .putLong(
                            LAST_SUCCESS,
                            syncNow
                        )
                        .apply()
                } catch (
                    security:
                        SecurityException
                ) {
                    Log.w(
                        "PesaTrackSmsSync",
                        "Recent SMS catch-up lost permission.",
                        security
                    )
                } catch (
                    failure: Exception
                ) {
                    Log.e(
                        "PesaTrackSmsSync",
                        "Recent SMS catch-up failed.",
                        failure
                    )
                } finally {
                    LedgerUpdateBus
                        .processingFinished(
                            changed
                        )
                    running.set(false)
                }
            },
            START_DELAY_MS,
            TimeUnit.MILLISECONDS
        )
    }
}

package com.kedevjimmy.pesatrack.ui.screens

import android.os.SystemClock
import android.util.Log

/**
 * Local performance timings for read-only financial screen queries.
 *
 * Only constant stage names and elapsed durations are logged, never personal
 * financial data. Does not cache, mutate, sample, or intercept exceptions.
 * Android logs are intended for a user-consented local diagnostic session.
 */
internal inline fun <T> traceFinancialRead(stage: String, read: () -> T): T {
    val startedAt = SystemClock.elapsedRealtime()
    try {
        return read()
    } finally {
        val elapsedMs = SystemClock.elapsedRealtime() - startedAt
        if (elapsedMs >= 120L) {
            Log.i("PesaTrackPerf", "$stage ${elapsedMs}ms")
        }
    }
}

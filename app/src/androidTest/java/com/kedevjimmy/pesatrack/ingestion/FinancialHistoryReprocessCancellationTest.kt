package com.kedevjimmy.pesatrack.ingestion

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import com.kedevjimmy.pesatrack.data.local.SmsAccessConsentStore
import org.junit.Rule
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** All SMS query results are invented in memory; the device inbox is never queried. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class FinancialHistoryReprocessCancellationTest {
    @get:Rule val smsPermission = GrantPermissionRule.grant(Manifest.permission.READ_SMS)
    private lateinit var context: Context
    private val now = 1_791_547_200_000L

    @Before fun setup() {
        val base: Context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        base.deleteDatabase("pesatrack.db")
        base.getSharedPreferences("financial_history_reprocess_state", Context.MODE_PRIVATE).edit().clear().commit()
        check(SmsAccessConsentStore.setAccepted(base, true))
        val provider = InventedSmsProvider(now)
        provider.attachInfo(base, ProviderInfo().apply { authority = "sms"; exported = false })
        val resolver = ContentResolver.wrap(provider)
        context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getContentResolver(): ContentResolver = resolver
        }
    }

    @After fun teardown() {
        SmsAccessConsentStore.setAccepted(context, false)
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        context.getSharedPreferences("financial_history_reprocess_state", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun cancelledScanDoesNotRecordSuccessfulReprocess() {
        val report = FinancialHistoryReprocessor(context).reprocess(
            months = 1, nowEpochMs = now, isCancelled = { true }
        )
        assertTrue(report.importResult.cancelled)
        assertEquals(0, report.importResult.scanned)
        assertNull(FinancialHistoryReprocessStateStore(context).snapshot().lastSuccessfulAtEpochMs)
    }

    @Test fun progressCancellationRetainsPartialLedgerAndRestartPreservesManualCorrection() {
        var cancel = false
        val progress = mutableListOf<HistoricalImportProgress>()
        val processor = FinancialHistoryReprocessor(context)
        val partial = processor.reprocess(
            months = 1, nowEpochMs = now,
            onProgress = { update ->
                progress += update
                if (update.scanned >= 1) cancel = true
            },
            isCancelled = { cancel }
        )
        assertTrue(partial.importResult.cancelled)
        assertTrue(progress.any { it.total == 80 && it.scanned == 1 })
        assertEquals(1, partial.importResult.scanned)
        assertNull(FinancialHistoryReprocessStateStore(context).snapshot().lastSuccessfulAtEpochMs)
        val store = TransactionStore.get(context)
        val first = requireNotNull(store.findByTransactionCode("TST12AB000"))
        PesaTrackDatabase.get(context).writableDatabase.update(
            "transactions", ContentValues().apply {
                put("category", "HEALTH")
                put("review_state", "USER_CLASSIFIED")
            }, "id = ?", arrayOf(first.id.toString())
        )
        val restarted = processor.reprocess(months = 1, nowEpochMs = now)
        assertFalse(restarted.importResult.cancelled)
        assertEquals(80, restarted.importResult.scanned)
        assertEquals("HEALTH", store.findByTransactionCode("TST12AB000")?.category)
        assertEquals("USER_CLASSIFIED", store.findByTransactionCode("TST12AB000")?.reviewState)
        val count = PesaTrackDatabase.get(context).readableDatabase.rawQuery("SELECT COUNT(*) FROM transactions", null)
            .use { it.moveToFirst(); it.getInt(0) }
        assertEquals(80, count)
        processor.reprocess(months = 1, nowEpochMs = now)
        val replayCount = PesaTrackDatabase.get(context).readableDatabase.rawQuery("SELECT COUNT(*) FROM transactions", null)
            .use { it.moveToFirst(); it.getInt(0) }
        assertEquals(count, replayCount)
        assertEquals(now, FinancialHistoryReprocessStateStore(context).snapshot().lastSuccessfulAtEpochMs)
    }

    private class InventedSmsProvider(private val now: Long) : ContentProvider() {
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor =
            MatrixCursor(arrayOf("_id", "address", "body", "date")).apply {
                repeat(80) { index ->
                    val code = "TST12AB" + index.toString().padStart(3, '0')
                    addRow(arrayOf<Any>(index.toLong(), "MPESA",
                        "$code Confirmed. Ksh 450.00 sent to SAMPLE PERSON 0712345678. Transaction cost, Ksh 0.00. New M-PESA balance is Ksh 1000.00.",
                        now - 80_000L + index * 1_000L))
                }
            }
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = error("Read-only invented provider")
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = error("Read-only invented provider")
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = error("Read-only invented provider")
    }
}

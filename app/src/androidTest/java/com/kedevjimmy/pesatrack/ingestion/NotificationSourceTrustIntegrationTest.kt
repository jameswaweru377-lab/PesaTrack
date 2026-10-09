package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.security.PesaBackupManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationSourceTrustIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var sources: NotificationSourceTrustStore
    @Before fun before() {
        context.getSharedPreferences(NotificationSourceTrustStore.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        sources = NotificationSourceTrustStore(context)
    }
    @After fun after() {
        PesaTrackDatabase.resetForTests()
        context.getSharedPreferences(NotificationSourceTrustStore.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun unapprovedSpoofCannotReachCanonicalIngestion() {
        val before = counts()
        sources.observe("ke.example.forwarder", "M-PESA")
        var entered = false
        sources.withApprovedSource("ke.example.forwarder") {
            entered = true
            ingestInventedPayment()
        }
        assertFalse(entered)
        assertEquals(before, counts())
        assertEquals("M-PESA", sources.observedSources().single().label)
    }

    @Test fun approvalPersistsAndRevocationStopsFinancialWrites() {
        sources.observe("ke.example.bank", "Invented Bank")
        assertTrue(sources.setApproved("ke.example.bank", true))
        val reopened = NotificationSourceTrustStore(context)
        val beforeApproval = counts()
        var entered = false
        reopened.withApprovedSource("ke.example.bank") { entered = true; ingestInventedPayment() }
        assertTrue(entered)
        assertTrue(counts().any { (name, count) -> count > beforeApproval.getValue(name) })
        val afterApproval = counts()
        assertTrue(reopened.setApproved("ke.example.bank", false))
        entered = false
        sources.withApprovedSource("ke.example.bank") { entered = true; ingestInventedPayment() }
        assertFalse(entered)
        assertEquals(afterApproval, counts())
    }

    @Test fun approvalRejectsSelfAndEmptyPackages() {
        assertFalse(sources.setApproved(context.packageName, true))
        assertFalse(sources.setApproved("", true))
    }

    @Test fun backupCannotReenableRevokedNotificationSource() {
        assertTrue(sources.setApproved("ke.example.bank", true))
        val backups = PesaBackupManager(context)
        val encrypted = backups.exportEncryptedBytes("invented-strong-passphrase")
        assertTrue(sources.setApproved("ke.example.bank", false))
        backups.restoreEncryptedBytes(encrypted, "invented-strong-passphrase")
        assertFalse(sources.isApproved("ke.example.bank"))
    }

    private fun ingestInventedPayment() {
        FinancialMessageIngestionCoordinator.get(context).ingest(
            title = "M-PESA",
            body = "QAB1CDE234 Confirmed. Ksh250.00 paid to INVENTED HARDWARE LTD. on 9/10/26 at 9:00 AM. New M-PESA balance is Ksh1,000.00. Transaction cost, Ksh0.00.",
            sourcePackage = "ke.example.bank",
            sourceKind = MpesaNotificationListenerService.SOURCE_FINANCIAL_NOTIFICATION,
            receivedAtEpochMs = 1_791_533_000_000L
        )
    }
    private fun counts(): Map<String, Long> {
        val db = PesaTrackDatabase.get(context).readableDatabase
        val names = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'", null).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        return names.associateWith { name ->
            db.rawQuery("SELECT COUNT(*) FROM \"${name.replace("\"", "\"\"")}\"", null).use { cursor -> cursor.moveToFirst(); cursor.getLong(0) }
        }
    }
}

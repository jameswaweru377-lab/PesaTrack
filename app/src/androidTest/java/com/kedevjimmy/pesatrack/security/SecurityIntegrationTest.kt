package com.kedevjimmy.pesatrack.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.data.local.RuleStore
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityIntegrationTest {
    private lateinit var context:
        Context

    @Before
    fun setUp() {
        context =
            ApplicationProvider
                .getApplicationContext()
        PesaTrackDatabase
            .resetForTests()
        context.deleteDatabase(
            "pesatrack.db"
        )
        context.getSharedPreferences(
            AppLockManager.PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        PesaTrackDatabase
            .resetForTests()
        context.getSharedPreferences(
            AppLockManager.PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun appLockHashesPinAndRequiresCorrectPin() {
        val lock =
            AppLockManager(context)

        assertFalse(
            lock.isEnabled()
        )

        lock.setPin("246810")

        assertTrue(
            lock.isEnabled()
        )
        assertTrue(
            lock.verifyPin("246810")
        )
        assertFalse(
            lock.verifyPin("135790")
        )
        assertFalse(
            lock.disable("135790")
        )
        assertTrue(
            lock.disable("246810")
        )
        assertFalse(
            lock.isEnabled()
        )
    }

    @Test
    fun appLockThrottlesRepeatedFailures() {
        val lock =
            AppLockManager(context)
        lock.setPin("246810")

        val start = 10_000L
        assertFalse(
            lock.verifyPin(
                "135790",
                start
            )
        )
        assertFalse(
            lock.verifyPin(
                "135790",
                start + 1L
            )
        )
        assertFalse(
            lock.verifyPin(
                "135790",
                start + 2L
            )
        )
        assertTrue(
            lock.remainingLockoutMs(
                start + 2L
            ) >= 2_000L
        )
        assertFalse(
            lock.verifyPin(
                "246810",
                start + 1_000L
            )
        )
        assertTrue(
            lock.verifyPin(
                "246810",
                start + 2_002L
            )
        )
        assertTrue(
            lock.remainingLockoutMs(
                start + 2_002L
            ) == 0L
        )
    }

    @Test(
        expected =
            IllegalArgumentException::class
    )
    fun appLockRejectsNewFourDigitPins() {
        AppLockManager(context)
            .setPin("2468")
    }

    @Test
    fun encryptedBackupRestoresDatabaseButNotAppLockPin() {
        val rules =
            RuleStore.get(context)
        val saved =
            rules.save(
                name = "Backup marker",
                counterpartyContains =
                    "TEST PAYEE",
                category =
                    TransactionCategory
                        .SHOPPING,
                direction =
                    MoneyDirection
                        .EXPENSE
            )
        val lock =
            AppLockManager(context)
        lock.setPin("246810")

        val backup =
            PesaBackupManager(
                context
            ).exportEncryptedBytes(
                "strong-passphrase"
            )

        assertTrue(
            rules.setEnabled(
                saved.id,
                false
            )
        )
        lock.setPin("567890")

        PesaBackupManager(
            context
        ).restoreEncryptedBytes(
            encrypted = backup,
            passphrase =
                "strong-passphrase"
        )

        assertTrue(
            RuleStore.get(context)
                .listRules(true)
                .any {
                    it.id == saved.id
                }
        )
        assertTrue(
            AppLockManager(context)
                .verifyPin("567890")
        )
        assertFalse(
            AppLockManager(context)
                .verifyPin("246810")
        )
    }

    @Test(
        expected =
            IllegalArgumentException::class
    )
    fun encryptedBackupRejectsWrongPassphrase() {
        val backup =
            PesaBackupManager(
                context
            ).exportEncryptedBytes(
                "correct-passphrase"
            )

        PesaBackupManager(
            context
        ).restoreEncryptedBytes(
            encrypted = backup,
            passphrase =
                "wrong-passphrase"
        )
    }
}

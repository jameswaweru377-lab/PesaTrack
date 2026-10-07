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

        lock.setPin("2468")

        assertTrue(
            lock.isEnabled()
        )
        assertTrue(
            lock.verifyPin("2468")
        )
        assertFalse(
            lock.verifyPin("1357")
        )
        assertFalse(
            lock.disable("1357")
        )
        assertTrue(
            lock.disable("2468")
        )
        assertFalse(
            lock.isEnabled()
        )
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
        lock.setPin("2468")

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
        lock.setPin("5678")

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
                .verifyPin("5678")
        )
        assertFalse(
            AppLockManager(context)
                .verifyPin("2468")
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

package com.kedevjimmy.pesatrack.ui.screens

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRestoreStateViewModelTest {
    @Test fun pendingSavePassphraseSurvivesViewRecreationWithoutSavedInstanceState() {
        val store = ViewModelStore()
        val first = ViewModelProvider(store, ViewModelProvider.NewInstanceFactory())[BackupRestoreStateViewModel::class.java]
        first.pendingCreatePassphrase.value = "temporary-secret-only-in-memory"

        val recreated = ViewModelProvider(store, ViewModelProvider.NewInstanceFactory())[BackupRestoreStateViewModel::class.java]

        assertSame(first, recreated)
        assertEquals("temporary-secret-only-in-memory", recreated.pendingCreatePassphrase.value)
        store.clear()
    }

    @Test fun oneRestoreCanStartUntilPreviousRestoreFinishes() {
        val state = BackupRestoreStateViewModel()

        assertTrue(state.claimRestore())
        assertFalse(state.claimRestore())
        assertTrue(state.busy.value)
        state.finishRestoreJob()
        assertFalse(state.busy.value)
        assertTrue(state.claimRestore())
    }

    @Test fun recreatedScreenCannotStartAnotherRestoreWhileOriginalIsRunning() {
        val store = ViewModelStore()
        val first = ViewModelProvider(store, ViewModelProvider.NewInstanceFactory())[BackupRestoreStateViewModel::class.java]
        assertTrue(first.claimRestore())

        val recreated = ViewModelProvider(store, ViewModelProvider.NewInstanceFactory())[BackupRestoreStateViewModel::class.java]
        assertFalse(recreated.claimRestore())
        assertTrue(recreated.busy.value)
        first.finishRestoreJob()
        assertTrue(recreated.claimRestore())
        store.clear()
    }

    @Test fun completedRestoreWaitsForActivityResumeAndTriggersOnlyOneReload() {
        val gate = BackupActivityReloadGate()
        gate.requestReload()

        assertTrue(gate.consumeReload())
        assertFalse(gate.consumeReload())
    }
}

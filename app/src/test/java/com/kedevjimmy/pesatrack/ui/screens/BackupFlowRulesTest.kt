package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFlowRulesTest {
    @Test
    fun newBackupsRequireTwelveCharacters() {
        assertEquals(
            "Use at least 12 characters for your new backup.",
            BackupFlowRules.createError("12345678901", "12345678901")
        )
        assertNull(BackupFlowRules.createError("one-long-passphrase", "one-long-passphrase"))
    }

    @Test
    fun createRequiresMatchingConfirmation() {
        assertEquals(
            "The passphrases do not match.",
            BackupFlowRules.createError("one-long-passphrase", "another-long-passphrase")
        )
    }

    @Test
    fun olderBackupsCanStillUseEightCharacterPassphrase() {
        assertNull(BackupFlowRules.restoreError("12345678"))
        assertEquals(
            "Enter the backup passphrase (at least 8 characters).",
            BackupFlowRules.restoreError("1234567")
        )
    }

    @Test
    fun restoringRequiresPreviewAndExplicitAcknowledgment() {
        assertFalse(BackupFlowRules.canRestore(false, true, false))
        assertFalse(BackupFlowRules.canRestore(true, false, false))
        assertFalse(BackupFlowRules.canRestore(true, true, true))
        assertTrue(BackupFlowRules.canRestore(true, true, false))
    }
}

package com.kedevjimmy.pesatrack.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPassphrasePolicyTest {
    @Test
    fun newBackupRejectsPassphrasesShorterThanTwelve() {
        listOf("", "1234567", "12345678", "12345678901").forEach { short ->
            assertFalse(BackupPassphrasePolicy.isValidForCreation(short))
            val error = runCatching {
                BackupPassphrasePolicy.requireForCreation(short)
            }.exceptionOrNull()
            assertTrue(error is IllegalArgumentException)
            assertEquals("Use at least 12 characters for your new backup.", error?.message)
        }
    }

    @Test
    fun newBackupAcceptsExactlyTwelveCharacters() {
        listOf("123456789012", "one-long-passphrase").forEach { passphrase ->
            assertTrue(BackupPassphrasePolicy.isValidForCreation(passphrase))
            BackupPassphrasePolicy.requireForCreation(passphrase)
        }
    }

    @Test
    fun existingBackupStillAcceptsEightCharacters() {
        assertTrue(BackupPassphrasePolicy.isValidForRestore("12345678"))
        BackupPassphrasePolicy.requireForRestore("12345678")
    }

    @Test
    fun existingBackupStillRejectsSevenCharacters() {
        assertFalse(BackupPassphrasePolicy.isValidForRestore("1234567"))
        val error = runCatching {
            BackupPassphrasePolicy.requireForRestore("1234567")
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertEquals("Use a backup passphrase with at least 8 characters.", error?.message)
    }
}

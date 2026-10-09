package com.kedevjimmy.pesatrack.security

import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSizePolicyTest {
    @Test
    fun encryptedBackupAtRestoreLimitIsAccepted() {
        BackupSizePolicy.requireRestorable(64L * 1024L * 1024L)
    }

    @Test
    fun encryptedBackupOneByteAboveRestoreLimitIsRefused() {
        val failure = runCatching {
            BackupSizePolicy.requireRestorable(64L * 1024L * 1024L + 1L)
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }
}

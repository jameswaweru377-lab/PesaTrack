package com.kedevjimmy.pesatrack.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedBackupIdentityTest {
    @Test fun sameEncryptedBytesMatchTheApprovedPreview() {
        val approved = EncryptedBackupIdentity.fromEncrypted("first backup".toByteArray())

        assertTrue(approved.matches("first backup".toByteArray()))
    }

    @Test fun changedEncryptedBytesAreRejectedEvenIfFileHasSameName() {
        val approved = EncryptedBackupIdentity.fromEncrypted("first backup".toByteArray())

        assertFalse(approved.matches("other backup".toByteArray()))
    }

    @Test fun identityCannotBeMutatedThroughTheOriginalInput() {
        val data = "first backup".toByteArray()
        val approved = EncryptedBackupIdentity.fromEncrypted(data)
        data[0] = 'X'.code.toByte()

        assertTrue(approved.matches("first backup".toByteArray()))
        assertFalse(approved.matches(data))
    }
}

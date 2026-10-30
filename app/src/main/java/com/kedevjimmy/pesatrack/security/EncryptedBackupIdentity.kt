package com.kedevjimmy.pesatrack.security

import java.security.MessageDigest

/** Identity of the exact encrypted bytes a person already previewed. */
internal class EncryptedBackupIdentity private constructor(private val digest: ByteArray) {
    fun matches(encrypted: ByteArray): Boolean =
        MessageDigest.isEqual(digest, MessageDigest.getInstance("SHA-256").digest(encrypted))

    companion object {
        fun fromEncrypted(encrypted: ByteArray): EncryptedBackupIdentity =
            EncryptedBackupIdentity(MessageDigest.getInstance("SHA-256").digest(encrypted))
    }
}

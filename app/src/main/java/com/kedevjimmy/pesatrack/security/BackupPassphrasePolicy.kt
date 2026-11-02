package com.kedevjimmy.pesatrack.security

/** Creation strength must not prevent unlocking backups created by older versions. */
internal object BackupPassphrasePolicy {
    private const val NEW_BACKUP_MIN_LENGTH = 12
    private const val LEGACY_RESTORE_MIN_LENGTH = 8

    fun isValidForCreation(passphrase: String): Boolean =
        passphrase.length >= NEW_BACKUP_MIN_LENGTH

    fun isValidForRestore(passphrase: String): Boolean =
        passphrase.length >= LEGACY_RESTORE_MIN_LENGTH

    fun requireForCreation(passphrase: String) {
        require(isValidForCreation(passphrase)) {
            "Use at least 12 characters for your new backup."
        }
    }

    fun requireForRestore(passphrase: String) {
        require(isValidForRestore(passphrase)) {
            "Use a backup passphrase with at least 8 characters."
        }
    }
}

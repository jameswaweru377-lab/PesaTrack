package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.security.BackupPassphrasePolicy

/** UI-only rules; older encrypted `.ptb` files keep their original restore policy. */
internal object BackupFlowRules {
    fun createError(passphrase: String, confirmation: String): String? = when {
        !BackupPassphrasePolicy.isValidForCreation(passphrase) ->
            "Use at least 12 characters for your new backup."
        passphrase != confirmation -> "The passphrases do not match."
        else -> null
    }

    fun restoreError(passphrase: String): String? =
        if (!BackupPassphrasePolicy.isValidForRestore(passphrase)) {
            "Enter the backup passphrase (at least 8 characters)."
        } else {
            null
        }

    fun canRestore(
        previewReady: Boolean,
        acknowledgedReplacement: Boolean,
        busy: Boolean
    ): Boolean = previewReady && acknowledgedReplacement && !busy
}

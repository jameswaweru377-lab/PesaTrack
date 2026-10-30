package com.kedevjimmy.pesatrack.ui.screens

/** UI-only rules; older encrypted `.ptb` files keep their original restore policy. */
internal object BackupFlowRules {
    fun createError(passphrase: String, confirmation: String): String? = when {
        passphrase.length < 12 -> "Use at least 12 characters for your new backup."
        passphrase != confirmation -> "The passphrases do not match."
        else -> null
    }

    fun restoreError(passphrase: String): String? =
        if (passphrase.length < 8) {
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

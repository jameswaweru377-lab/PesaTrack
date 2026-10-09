package com.kedevjimmy.pesatrack.security

/** Shared byte-count contract for export, preview and restore; no large allocations required. */
internal object BackupSizePolicy {
    const val MAX_ENCRYPTED_BYTES = 64L * 1024L * 1024L

    fun requireRestorable(encryptedBytes: Long) {
        require(encryptedBytes in 1L..MAX_ENCRYPTED_BYTES) {
            "This backup exceeds the 64 MiB recovery limit. Reduce stored history before exporting."
        }
    }
}

package com.kedevjimmy.pesatrack.ui.screens

internal enum class ProfileSaveResult { SAVED, INVALID_EMAIL, FAILED }

/** A success message is only valid after the local preference write succeeds. */
internal fun attemptProfileSave(
    name: String,
    avatar: String,
    email: String,
    emailValid: (String) -> Boolean,
    persist: (String, String, String) -> Boolean
): ProfileSaveResult {
    val cleanEmail = email.trim()
    if (cleanEmail.isNotEmpty() && !emailValid(cleanEmail)) {
        return ProfileSaveResult.INVALID_EMAIL
    }
    return if (persist(name.trim(), avatar, cleanEmail)) {
        ProfileSaveResult.SAVED
    } else {
        ProfileSaveResult.FAILED
    }
}

package com.kedevjimmy.pesatrack.ui.screens

internal enum class ProfileSaveResult { SAVED, INVALID_NAME, FAILED }

/** A success message is only valid after the local preference write succeeds. */
internal fun attemptProfileSave(
    name: String,
    avatar: String,
    city: String,
    bio: String,
    persist: (String, String, String, String) -> Boolean
): ProfileSaveResult {
    val cleanName = name.trim()
    if (cleanName.isEmpty()) return ProfileSaveResult.INVALID_NAME
    return if (persist(cleanName, avatar, city.trim(), bio.trim())) {
        ProfileSaveResult.SAVED
    } else {
        ProfileSaveResult.FAILED
    }
}

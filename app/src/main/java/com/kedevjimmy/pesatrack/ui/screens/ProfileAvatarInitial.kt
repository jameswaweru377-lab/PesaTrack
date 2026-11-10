package com.kedevjimmy.pesatrack.ui.screens

/** Honour an explicitly chosen avatar; otherwise use the name or an offline placeholder. */
internal fun profileAvatarLabel(name: String, savedAvatar: String): String {
    val explicitAvatar = savedAvatar.trim().takeUnless { it.isEmpty() || it == "🌿" }
    if (explicitAvatar != null) return explicitAvatar
    return name.trim().firstOrNull { it.isLetter() }
        ?.uppercaseChar()?.toString() ?: "J"
}

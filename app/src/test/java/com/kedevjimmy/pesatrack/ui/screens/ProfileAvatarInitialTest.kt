package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileAvatarInitialTest {
    @Test fun defaultsToJWithoutProfileDetails() {
        assertEquals("J", profileAvatarLabel("", "🌿"))
    }

    @Test fun usesTheSavedNameForTheDefaultAvatar() {
        assertEquals("J", profileAvatarLabel(" James Waweru ", "🌿"))
        assertEquals("A", profileAvatarLabel("amina", "🌿"))
    }

    @Test fun preservesExplicitlyChosenAvatars() {
        assertEquals("🦁", profileAvatarLabel("James Waweru", "🦁"))
        assertEquals("K", profileAvatarLabel("", "K"))
    }
}

package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileDetailsFeedbackPolicyTest {
    @Test fun successOnlyAfterPersistenceAndValuesAreTrimmed() {
        var recorded: List<String>? = null
        val result = attemptProfileSave(
            name = "  James Waweru  ",
            avatar = "🌿",
            city = "  Nairobi  ",
            bio = "  Keeping my finances on track.  ",
            persist = { name, avatar, city, bio ->
                recorded = listOf(name, avatar, city, bio)
                true
            }
        )
        assertEquals(ProfileSaveResult.SAVED, result)
        assertEquals(listOf("James Waweru", "🌿", "Nairobi", "Keeping my finances on track."), recorded)
    }

    @Test fun emptyNameNeverCallsStorage() {
        var called = false
        val result = attemptProfileSave("   ", "🌿", "Nairobi", "",
            persist = { _, _, _, _ -> called = true; true })
        assertEquals(ProfileSaveResult.INVALID_NAME, result)
        assertFalse(called)
    }

    @Test fun failedWriteNeverClaimsSavedAndOptionalDetailsAreAllowed() {
        var called = false
        val result = attemptProfileSave("Name", "🌿", "", "",
            persist = { _, _, _, _ -> called = true; false })
        assertTrue(called)
        assertEquals(ProfileSaveResult.FAILED, result)
    }
}

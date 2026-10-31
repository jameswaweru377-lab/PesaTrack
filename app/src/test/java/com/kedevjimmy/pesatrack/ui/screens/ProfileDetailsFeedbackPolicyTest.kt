package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileDetailsFeedbackPolicyTest {
    @Test fun successOnlyAfterPersistenceAndValuesAreTrimmed() {
        var recorded: Triple<String, String, String>? = null
        val result = attemptProfileSave(
            name = "  James Waweru  ",
            avatar = "🌿",
            email = "  hello@example.com  ",
            emailValid = { it == "hello@example.com" },
            persist = { name, avatar, email ->
                recorded = Triple(name, avatar, email)
                true
            }
        )
        assertEquals(ProfileSaveResult.SAVED, result)
        assertEquals(Triple("James Waweru", "🌿", "hello@example.com"), recorded)
    }

    @Test fun invalidEmailNeverCallsStorage() {
        var called = false
        val result = attemptProfileSave("Name", "🌿", "not-an-email",
            emailValid = { false }, persist = { _, _, _ -> called = true; true })
        assertEquals(ProfileSaveResult.INVALID_EMAIL, result)
        assertFalse(called)
    }

    @Test fun failedWriteNeverClaimsSavedAndEmptyEmailIsAllowed() {
        var called = false
        val result = attemptProfileSave("Name", "🌿", "",
            emailValid = { false }, persist = { _, _, _ -> called = true; false })
        assertTrue(called)
        assertEquals(ProfileSaveResult.FAILED, result)
    }
}

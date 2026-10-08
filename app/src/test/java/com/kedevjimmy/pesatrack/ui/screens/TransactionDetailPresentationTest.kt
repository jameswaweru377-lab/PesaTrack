package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionDetailPresentationTest {
    @Test
    fun bankInitialsUseInstitutionIdentityInsteadOfPersonAvatar() {
        assertEquals(
            "SB",
            bankInitials(
                "STANBIC BANK KENYA for account"
            )
        )
        assertEquals(
            "KCB",
            bankInitials(
                "KCB Bank Kenya"
            )
        )
        assertEquals(
            "SC",
            bankInitials(
                "Standard Chartered Bank Kenya"
            )
        )
        assertNull(
            bankInitials(
                "Elizabeth"
            )
        )
    }
}

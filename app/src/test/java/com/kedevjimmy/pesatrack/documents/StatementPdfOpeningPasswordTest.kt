package com.kedevjimmy.pesatrack.documents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StatementPdfOpeningPasswordTest {
    @Test fun encryptedPdfRequiresASeparatePrivatePasswordPrompt() {
        val blocked = assertThrows(StatementPdfOpeningPasswordRequiredException::class.java) {
            StatementPdfOpeningPassword.requireUnlocked("blocked", "pdf_password_rejected")
        }
        assertEquals("This PDF needs its opening password.", blocked.message)
    }

    @Test fun ordinaryAndUnsupportedPdfsDoNotTriggerPasswordPrompt() {
        listOf(
            "ready_for_bank_review" to "",
            "ready_for_separate_activity_review" to "",
            "blocked" to "unsupported_or_unverified_pdf",
            "blocked" to "pdf_open_failed",
            "blocked" to "unverified_statement_coverage",
            "ready_for_bank_review" to "pdf_password_rejected"
        ).forEach { (status, reason) ->
            StatementPdfOpeningPassword.requireUnlocked(status, reason)
        }
    }
}

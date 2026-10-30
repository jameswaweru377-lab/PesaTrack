package com.kedevjimmy.pesatrack.documents

/** No file metadata, password or private PDF text is exposed in this error. */
class StatementPdfOpeningPasswordRequiredException : Exception("This PDF needs its opening password.")

/** Only an explicit password failure can request a password; no generic parser error may do so. */
object StatementPdfOpeningPassword {
    fun requireUnlocked(status: String, reason: String) {
        if (status == "blocked" && reason == "pdf_password_rejected") {
            throw StatementPdfOpeningPasswordRequiredException()
        }
    }
}

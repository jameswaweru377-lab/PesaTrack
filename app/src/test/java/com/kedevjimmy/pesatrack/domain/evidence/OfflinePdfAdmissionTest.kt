package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.*
import org.junit.Test

class OfflinePdfAdmissionTest {
    @Test fun acceptsPdfSignatureWithoutAssumingFinancialAuthenticity() {
        OfflinePdfAdmission.requireSupportedFile("%PDF-1.7\nfictional data".toByteArray())
    }

    @Test fun rejectsRenamedTextAndTruncatedDocuments() {
        for (value in listOf("".toByteArray(), "PK\\x03\\x04".toByteArray(),
            "%PDF".toByteArray(), "HTML document".toByteArray())) {
            assertThrows(IllegalArgumentException::class.java) {
                OfflinePdfAdmission.requireSupportedFile(value)
            }
        }
    }

    @Test fun rejectsOversizedInputsWithoutProcessingThem() {
        assertThrows(IllegalArgumentException::class.java) {
            OfflinePdfAdmission.requireSupportedFile(
                ByteArray(OfflinePdfAdmission.MAX_INPUT_BYTES + 1)
            )
        }
    }
}

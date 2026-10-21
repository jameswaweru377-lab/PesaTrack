package com.kedevjimmy.pesatrack.domain.evidence

/** Quick rejection before giving a user-selected file to the offline Python PDF parser.
 * This is NOT a malware scanner or proof that a document is safe/financially valid.
 * Parsed contents and financial totals still require independent verification.
 */
object OfflinePdfAdmission {
    private val magic = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D) // %PDF-
    const val MAX_INPUT_BYTES = 8 * 1024 * 1024

    fun requireSupportedFile(bytes: ByteArray) {
        require(bytes.size in magic.size..MAX_INPUT_BYTES) {
            "The PDF is empty or exceeds the 8 MB local import limit."
        }
        require(magic.indices.all { bytes[it] == magic[it] }) {
            "The selected document is not a PDF. No records were saved."
        }
    }
}

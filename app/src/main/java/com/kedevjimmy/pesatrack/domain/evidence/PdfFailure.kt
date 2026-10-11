package com.kedevjimmy.pesatrack.domain.evidence

/** Fixed, non-sensitive failures only. Never display native exceptions or document contents. */
enum class PdfFailure(val explanation: String) {
    EMPTY("The selected file is empty. Download the PDF again."),
    TOO_LARGE("This PDF exceeds 3 MB. Request a shorter statement or a smaller PDF."),
    NOT_PDF("The selected file does not have a supported PDF header. Download the original PDF and try again."),
    ACCESS("The app could not open this file. Save it to Downloads, then choose that copy."),
    COPY("The app could not finish reading this file. Download it completely, then choose it from Downloads."),
    PASSWORD("This PDF needs a password, or uses unsupported encryption. Enter its document password to try unlocking it locally."),
    TOO_MANY_PAGES("This PDF exceeds 40 pages. Choose a shorter statement."),
    TOO_MUCH_TEXT("This PDF exceeds 100,000 extracted text characters. Choose a shorter document."),
    NO_TEXT("No selectable text was found. This PDF may contain images or unsupported text encoding. OCR is not available; enter the details manually."),
    PARSE("The PDF reader could not decode this document. Its format may be damaged or unsupported. Try downloading the original again."),
    SERVICE("The local PDF reader could not start or stopped unexpectedly. Reopen import and try again."),
    TIMEOUT("PDF reading took too long. Choose a smaller document and try again."),
    VERSION("PDF extraction needs Android 15 or newer. CSV and manual entry remain available.")
}
class PdfReadException(val failure: PdfFailure) : IllegalArgumentException(failure.explanation)

package com.kedevjimmy.pesatrack.domain.evidence

import com.kedevjimmy.pesatrack.domain.statements.StatementField

/** Authored check labels only. Never put private cell text in a failure. */
enum class PdfStatementCheck(val explanation:String) {
    INCOMPLETE_GEOMETRY("Some text has incomplete physical coordinates."),
    HEADER_DISCOVERY("The required table headings could not be located."),
    RECEIPT_COVERAGE("Source receipts, completed labels and positioned receipt anchors do not agree."),
    ROW_BOUNDARY("Transaction evidence appears outside a supported row boundary."),
    ROW_SIZE("The row is empty or contains more than the supported number of text parts."),
    COLUMN_OWNERSHIP("A text rectangle cannot be assigned wholly to one column."),
    REFERENCE_CELL("The reference cell does not match the source receipt."),
    SOURCE_TIMESTAMP_COUNT("The source receipt block does not contain exactly one completion time."),
    TIMESTAMP_CELL("The date/time cell does not match the dated source receipt."),
    STATUS_CELL("The status cell is not one complete Completed label."),
    DESCRIPTION_LENGTH("The description is empty or exceeds the supported 200 characters."),
    DESCRIPTION_STATUS("A completed-status label appears inside the description column."),
    CREDIT_PARTS("Paid In contains multiple positioned text parts; one amount has not been proved."),
    CREDIT_AMOUNT("Paid In does not contain a supported non-negative amount."),
    DEBIT_PARTS("Withdrawn contains multiple positioned text parts; one amount has not been proved."),
    DEBIT_AMOUNT("Withdrawn does not contain a supported amount."),
    BALANCE_PARTS("Balance contains multiple positioned text parts; one amount has not been proved."),
    BALANCE_AMOUNT("Balance is missing or does not contain a supported non-negative amount."),
    MOVEMENT_DIRECTION("Paid In and Withdrawn are both non-zero in the same row."),
    ZERO_EVIDENCE("Empty money cells do not independently prove a zero-value record."),
    SUMMARY_EVIDENCE("The observed summary table could not be verified."),
    FINANCIAL_RECONCILIATION("Receipt groups, dates, balances or summary declarations did not reconcile.");

    companion object {
        private val roles=listOf(StatementField.REFERENCE,StatementField.DATE,StatementField.DESCRIPTION,
            StatementField.STATUS,StatementField.CREDIT,StatementField.DEBIT,StatementField.BALANCE)
        private val labels=listOf("Receipt", "Date/time", "Description", "Status", "Paid In", "Withdrawn", "Balance")
        fun decode(name:String?):PdfStatementCheck?=entries.firstOrNull {it.name==name}
        fun encodeColumns(columns:Set<StatementField>):Int=roles.indices.fold(0) {mask,i->
            if(roles[i] in columns)mask or (1 shl i) else mask}
        fun decodeColumns(mask:Int):Set<StatementField> =
            if(mask !in 0..127)emptySet() else roles.indices.filter {mask and (1 shl it)!=0}.map {roles[it]}.toSet()
        fun columnLabels(columns:Set<StatementField>):String=roles.indices.filter {roles[it] in columns}
            .joinToString(" / "){labels[it]}
    }
}

/** Fixed, content-free classification of an unowned positioned text box. */
enum class PdfColumnDiagnostic(val explanation:String) {
    VERIFIED_TIMESTAMP_TOO_WIDE("The source-verified Date/time rectangle extends too far into another column."),
    VERIFIED_TIMESTAMP_TOUCHES_NEIGHBOR("The source-verified Date/time rectangle overlaps adjacent text."),
    VERIFIED_TIMESTAMP_LAYOUT_MISMATCH("The verified Date/time rectangle does not fit the recognized neighboring columns."),
    COMBINED_OR_UNVERIFIED_TEXT("The crossing text is not a separately source-verified date or time token."),
    DESCRIPTION_CROSSES_BOUNDARY("A Description text rectangle crosses into the Date/time region."),
    OTHER_RECTANGLE("Another text rectangle cannot be assigned to an observed column.");

    companion object {
        fun decode(name:String?):PdfColumnDiagnostic?=entries.firstOrNull {it.name==name}
    }
}

/** Fixed, non-sensitive failures only. Never display native exceptions or document contents. */
enum class PdfFailure(val explanation: String) {
    EMPTY("The selected file is empty. Download the PDF again."),
    TOO_LARGE("This PDF exceeds 8 MB. Choose a shorter statement or a smaller PDF."),
    NOT_PDF("The selected file does not have a supported PDF header. Download the original PDF and try again."),
    ACCESS("The app could not open this file. Save it to Downloads, then choose that copy."),
    COPY("The app could not finish reading this file. Download it completely, then choose it from Downloads."),
    PASSWORD("This PDF needs a password, or uses unsupported encryption. Enter its document password to try unlocking it locally."),
    TOO_MANY_PAGES("This PDF exceeds 160 pages. Choose a shorter statement."),
    TOO_MUCH_TEXT("This PDF exceeds 500,000 extracted text characters. Choose a shorter date range."),
    NO_TEXT("No selectable text was found. This PDF may contain images or unsupported text encoding. OCR is not available; enter the details manually."),
    PARSE("The PDF reader could not decode this document. Its format may be damaged or unsupported. Try downloading the original again."),
    TABLE_EVIDENCE("The PDF opened, but its complete receipt rows and physical money columns could not be verified. Nothing was imported."),
    METADATA_EVIDENCE("The table was read, but the required statement title, period headings or currency evidence could not be verified. Nothing was imported."),
    FINANCIAL_EVIDENCE("The PDF table was reconstructed, but its receipt groups, running balances or declared summary totals did not reconcile completely. Nothing was imported."),
    SERVICE("The local PDF reader could not start or stopped unexpectedly. Reopen import and try again."),
    TIMEOUT("PDF reading took too long. Choose a smaller document and try again."),
    VERSION("PDF extraction needs Android 15 or newer. CSV and manual entry remain available.")
}
class PdfReadException(val failure: PdfFailure, val statementPage: Int? = null,val statementRow:Int?=null,
    val statementCheck:PdfStatementCheck?=null,val statementColumns:Set<StatementField> = emptySet(),
    val columnDiagnostic:PdfColumnDiagnostic? = null) :
    IllegalArgumentException(failure.explanation) {
    /** Bounded location and whitelisted authored labels, never private text. */
    val safeExplanation: String get() = failure.explanation +
        (statementPage?.takeIf {it in 1..160}?.let {page->" Affected page: $page"+
            (statementRow?.takeIf {it in 1..5000}?.let {", receipt row: $it"} ?: "")+"."} ?: "") +
        (statementCheck?.let {" Check: ${it.explanation}"} ?: "") +
        (if(statementCheck==PdfStatementCheck.COLUMN_OWNERSHIP)
            PdfStatementCheck.columnLabels(statementColumns).takeIf {it.isNotEmpty()}?.let {" Columns: $it."}.orEmpty() +
            (columnDiagnostic?.let { " Detail: ${it.explanation}" } ?: "")
            else "")
}

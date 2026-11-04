package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import java.io.Writer
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

/** Preserve the previous, opt-in CSV export alongside the newly approved PDF. */
internal fun reportCsvCell(value: String): String {
    val safe = if (value.firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r')) "'$value" else value
    return "\"" + safe.replace("\"", "\"\"") + "\""
}

internal fun writePesaTrackReportCsv(rows: List<StoredTransaction>, writer: Writer) {
    writer.appendLine("Date,Payee,Direction,Category,Amount KES,Fee KES")
    val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    rows.forEach { row ->
        val cells = listOf(
            date.format(row.occurredAtEpochMs), row.counterparty.orEmpty(),
            row.direction, row.category,
            BigDecimal.valueOf(row.amountMinor, 2).toPlainString(),
            BigDecimal.valueOf(row.feeMinor, 2).toPlainString()
        )
        writer.appendLine(cells.joinToString(",", transform = ::reportCsvCell))
    }
}

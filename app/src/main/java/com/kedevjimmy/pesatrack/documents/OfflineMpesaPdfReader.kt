package com.kedevjimmy.pesatrack.documents

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.kedevjimmy.pesatrack.domain.everyday.StatementActivityLine
import com.kedevjimmy.pesatrack.domain.everyday.StatementActivityProof
import java.io.ByteArrayOutputStream
import org.json.JSONObject

/**
 * Offline Python parser bridge. It never calls EverydayMoneyStore, SMS
 * ingestion, budgeting, network services, or ledger update methods.
 *
 * The original PDF/password stay on-device and in-memory only. The source
 * Uri is opened read-only; the returned rows can only be saved to the
 * isolated Statement Activity journal after explicit user approval.
 */
object OfflineMpesaPdfReader {
    private const val MAX_PDF_BYTES = 8 * 1024 * 1024

    data class Prepared(
        val lines: List<StatementActivityLine>,
        val proof: StatementActivityProof,
        val pages: Int,
        val paidInMinor: Long,
        val paidOutMinor: Long,
        val categoryPaidInGapMinor: Long,
        val categoryPaidOutGapMinor: Long,
        val fulizaCycles: Int,
        val sharedBalanceGroups: Int,
        val sourceCharacters: Int
    ) {
        val informationalRows: Int get() = lines.count { it.informational }
    }

    private fun boundedBytes(context: Context, uri: Uri): ByteArray {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val chunk = ByteArray(16384)
            while (true) {
                val n = input.read(chunk)
                if (n < 0) break
                require(output.size() + n <= MAX_PDF_BYTES) { "PDF exceeds the 8 MB local review limit." }
                output.write(chunk, 0, n)
            }
            val data = output.toByteArray()
            require(data.isNotEmpty()) { "The PDF file is empty." }
            return data
        }
        error("Could not open the selected PDF.")
    }

    /** Invoke on a background dispatcher; parsing 112 pages may take time. */
    fun inspect(context: Context, uri: Uri, password: String): Prepared {
        require(password.length <= 256)
        val raw = boundedBytes(context, uri)
        try {
            val app = context.applicationContext
            synchronized(Python::class.java) {
                if (!Python.isStarted()) Python.start(AndroidPlatform(app))
            }
            val base64 = Base64.encodeToString(raw, Base64.NO_WRAP)
            val response = Python.getInstance().getModule("mpesa_pdf_engine")
                .callAttr("inspect_pdf", base64, password).toString()
            val root = JSONObject(response)
            require(root.optString("status") == "ready_for_separate_activity_review") {
                "PDF did not pass complete local source, totals and balance verification (" +
                    root.optString("reason", "unsupported format") + "). No records were saved."
            }
            val result = root.getJSONObject("report")
            require(result.getBoolean("summaryDetailedTotalsMatch") &&
                result.getBoolean("wallet_chain_unique_provisionally") &&
                !result.getBoolean("verified_for_sms_ledger_import"))
            val sourceRows = result.getInt("sourceRows")
            val financial = result.getInt("financialRows")
            val informational = result.getInt("informationalRows")
            val pages = result.getInt("pages")
            require(pages in 1..160 && sourceRows in 1..5000 &&
                financial > 0 && informational >= 0 && financial + informational == sourceRows &&
                result.getInt("sourceCharacters") > 0)
            val items = root.getJSONArray("lines")
            require(items.length() == sourceRows)
            val lines = (0 until sourceRows).map { i ->
                val line = items.getJSONObject(i)
                StatementActivityLine(
                    page = line.getInt("page"),
                    row = line.getInt("row"),
                    reference = line.getString("reference"),
                    occurredAt = line.getLong("occurredAt"),
                    description = line.getString("description"),
                    paidInMinor = line.getLong("paidInMinor"),
                    withdrawnMinor = line.getLong("withdrawnMinor"),
                    balanceMinor = line.getLong("balanceMinor"),
                    sourceCells = line.getJSONArray("sourceCells").let { cells ->
                        require(cells.length() == 7) { "Every statement row must retain seven source cells." }
                        (0 until 7).map { cell -> cells.getString(cell) }
                    }
                )
            }
            require(lines.count { it.informational } == informational)
            require(lines.zipWithNext().all { (a,b) ->
                a.occurredAt >= b.occurredAt &&
                    (a.page < b.page || (a.page == b.page && a.row < b.row))
            })
            val paid = lines.fold(0L) { acc, row -> Math.addExact(acc, row.paidInMinor) }
            val withdrawn = lines.fold(0L) { acc, row -> Math.addExact(acc, row.withdrawnMinor) }
            require(paid == result.getLong("paidInMinor") &&
                withdrawn == result.getLong("paidOutMinor"))
            val net = Math.subtractExact(paid, withdrawn)
            require(net == Math.subtractExact(
                result.getLong("derived_closing_minor"),
                result.getLong("derived_opening_minor")
            )) { "Full financial movement does not reconcile." }
            return Prepared(
                lines = lines,
                proof = StatementActivityProof(sourceRows, financial, true),
                pages = pages,
                paidInMinor = paid,
                paidOutMinor = withdrawn,
                categoryPaidInGapMinor = result.getLong("categoryPaidInDiscrepancyMinor"),
                categoryPaidOutGapMinor = result.getLong("categoryPaidOutDiscrepancyMinor"),
                fulizaCycles = result.getInt("fuliza_separately_checked"),
                sharedBalanceGroups = result.getInt("shared_ending_balance_receipts"),
                sourceCharacters = result.getInt("sourceCharacters")
            )
        } finally {
            raw.fill(0)
        }
    }
}

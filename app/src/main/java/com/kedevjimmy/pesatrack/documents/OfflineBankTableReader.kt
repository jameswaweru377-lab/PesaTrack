package com.kedevjimmy.pesatrack.documents

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.kedevjimmy.pesatrack.domain.evidence.OfflinePdfAdmission
import com.kedevjimmy.pesatrack.domain.statements.StatementDocumentProof
import com.kedevjimmy.pesatrack.domain.statements.StatementDocumentAssurance
import com.kedevjimmy.pesatrack.domain.statements.StatementPrintedTotal
import com.kedevjimmy.pesatrack.domain.statements.StatementTablePage
import java.io.ByteArrayOutputStream
import org.json.JSONObject

/** Stable parser refusal; never contains raw PDF text or account identifiers. */
class StatementBankInspectionBlocked(val reasonCode: String) : IllegalArgumentException(
    "This bank PDF did not pass complete local verification ($reasonCode). Nothing was saved."
)

/** Read-only on-device issuer table review; no Activity, network or database writes. */
object OfflineBankTableReader {
    data class Prepared(
        val document: StatementDocumentProof,
        val tables: List<StatementTablePage>,
        val sourceRows: Int
    )

    private fun readBounded(context: Context, uri: Uri): ByteArray {
        val output = ByteArrayOutputStream()
        context.contentResolver.openInputStream(uri)?.use { input ->
            val chunk = ByteArray(16_384)
            while (true) {
                val count = input.read(chunk)
                if (count < 0) break
                require(output.size() + count <= OfflinePdfAdmission.MAX_INPUT_BYTES) {
                    "This PDF exceeds the 8 MB local verification limit."
                }
                output.write(chunk, 0, count)
            }
        } ?: error("The selected PDF could not be opened.")
        val raw = output.toByteArray()
        OfflinePdfAdmission.requireSupportedFile(raw)
        return raw
    }

    /** Invoke only from Dispatchers.IO. Caller keeps the password out of saved state. */
    fun inspect(context: Context, uri: Uri, password: String, providerKey: String): Prepared {
        require(password.length <= 256)
        val raw = readBounded(context, uri)
        val response = try {
            synchronized(Python::class.java) {
                if (!Python.isStarted()) Python.start(AndroidPlatform(context.applicationContext))
            }
            Python.getInstance().getModule("bank_table_extractor")
                .callAttr("inspect_pdf", Base64.encodeToString(raw, Base64.NO_WRAP), password, providerKey)
                .toString()
        } finally { raw.fill(0) }
        val root = JSONObject(response)
        StatementPdfOpeningPassword.requireUnlocked(root.optString("status"), root.optString("reason"))
        if (root.optString("status") != "ready_for_bank_review") {
            val rawReason = root.optString("reason")
            val safeReason = rawReason.takeIf { it.matches(Regex("[a-z][a-z0-9_]{0,63}")) }
                ?: "unknown_failure"
            throw StatementBankInspectionBlocked(safeReason)
        }
        val source = root.getJSONObject("document")
        val count = source.getInt("pages")
        require(count in 1..160 && source.getString("providerKey") == providerKey)
        val totals = source.getJSONObject("printedPageTotals")
        val perPage = buildMap {
            totals.keys().forEach { key ->
                val page = key.toInt()
                require(page in 1..count && put(page, totals.getJSONObject(key).let {
                    StatementPrintedTotal(it.getLong("moneyOutMinor"), it.getLong("moneyInMinor"))
                }) == null)
            }
        }
        val grand = source.getJSONObject("printedGrandTotal")
        val proof = StatementDocumentProof(
            detectedProviderKey = source.getString("providerKey"),
            detectedAccountIdentifier = source.getString("accountIdentifier"),
            currency = source.getString("currency"), format = source.getString("format"),
            allSourceRegionsAccountedFor = source.getBoolean("allSourceRegionsAccountedFor"),
            pages = count, printedPageTotals = perPage,
            printedGrandTotal = StatementPrintedTotal(grand.getLong("moneyOutMinor"),
                grand.getLong("moneyInMinor")),
            openingBalanceMinor = if (source.isNull("openingBalanceMinor")) null
                else source.getLong("openingBalanceMinor"),
            assurance = when (source.optString("assurance")) {
                "grand_total_and_chain_only" -> StatementDocumentAssurance.GRAND_TOTAL_AND_CHAIN_ONLY
                "" -> StatementDocumentAssurance.FULL
                else -> error("Unknown bank document assurance; no data saved.")
            }
        )
        val physical = root.getJSONArray("tables")
        require(physical.length() in 1..160)
        val pages = (0 until physical.length()).map { index ->
            val table = physical.getJSONObject(index)
            val headers = table.getJSONArray("headers")
            require(headers.length() in 3..40)
            val headings = List(headers.length()) { headers.getString(it) }
            val rows = table.getJSONArray("rows")
            require(rows.length() in 1..5_000)
            StatementTablePage(table.getInt("pageNumber"), headings,
                List(rows.length()) { row ->
                    val cells = rows.getJSONArray(row)
                    require(cells.length() == headings.size)
                    List(headings.size) { cell -> cells.getString(cell) }
                })
        }
        val totalRows = pages.sumOf { it.rows.size }
        require(totalRows in 1..5_000)
        return Prepared(proof, pages, totalRows)
    }
}

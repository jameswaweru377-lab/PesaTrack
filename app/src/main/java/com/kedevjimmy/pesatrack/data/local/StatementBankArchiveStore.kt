package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.statements.StatementAdmissionResult
import com.kedevjimmy.pesatrack.domain.statements.StatementTablePage
import com.kedevjimmy.pesatrack.domain.statements.StatementTableResult
import com.kedevjimmy.pesatrack.domain.statements.UniversalStatementTableInterpreter
import java.nio.ByteBuffer
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/** Evidence-only bank archive. Never touches Activity transactions or balances. */
class StatementBankArchiveStore(context: Context) {
    private val app = context.applicationContext

    data class Source(
        val id: String, val accountId: String, val providerKey: String,
        val currency: String, val format: String, val importedAt: Long,
        val firstAt: Long, val lastAt: Long,
        val rows: Int, val moneyInMinor: Long, val moneyOutMinor: Long
    )
    data class Leg(
        val sourceId: String, val number: Int, val page: Int, val row: Int,
        val occurredAt: Long, val description: String, val reference: String?,
        val movementMinor: Long, val runningBalanceMinor: Long,
        val originalHeaders: List<String>, val originalCells: List<String>,
        val categoryId: String?
    )
    data class SaveResult(val added: Boolean, val sourceId: String)

    /** A statement must match a confirmed account, not simply a bank-shaped name. */
    private fun requireConfirmedBank(accountId: String, accountIdentity: String) {
        val confirmed = FinancialUnderstandingStore.get(app).confirmedAccountIds() +
            AccountOwnershipConsentStore(app).confirmedRevisions().keys
        val account = EverydayMoneyStore(app).ownedAccounts().singleOrNull {
            it.id == accountId && it.type == AccountType.BANK && it.id in confirmed
        } ?: error("Confirm the bank account in Accounts before importing its statement.")
        val normalized = account.identifier.orEmpty().filter(Char::isDigit)
        require(normalized.length >= 8 && normalized == accountIdentity) {
            "The statement does not match this confirmed bank account."
        }
    }

    private fun fingerprint(admission: StatementAdmissionResult.Accepted, pages: List<StatementTablePage>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fun feed(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array())
            digest.update(bytes)
        }
        feed(admission.accountId)
        feed(admission.profile.providerKey)
        feed(admission.profile.currency)
        feed(admission.accountIdentityHashMaterial)
        pages.forEach { page ->
            feed(page.pageNumber.toString()); feed(page.tableNumber.toString())
            page.headers.forEach(::feed)
            page.rows.forEach { row -> row.forEach(::feed) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun save(admitted: StatementAdmissionResult.Accepted, pages: List<StatementTablePage>): SaveResult {
        requireConfirmedBank(admitted.accountId, admitted.accountIdentityHashMaterial)
        require(admitted.profile.sampleValidatedFormats.contains(admitted.documentFormat)) {
            "This statement format has not been verified on this device."
        }
        require(admitted.sourceRows in 1..5_000 && admitted.legs.size == admitted.sourceRows)
        require(pages.isNotEmpty() && pages.flatMap { it.rows }.size == admitted.sourceRows)
        val replayed = UniversalStatementTableInterpreter.interpret(
            pages, openingBalanceMinor = admitted.openingBalanceMinor, profile = admitted.profile.table
        ) as? StatementTableResult.Accepted
        require(replayed != null && replayed.legs == admitted.legs &&
            replayed.sourceRows == admitted.sourceRows) {
            "Source rows changed after admission."
        }
        val cellByPosition = buildMap {
            pages.forEach { page ->
                require(page.headers.size in 3..40)
                page.rows.forEachIndexed { index, row ->
                    require(row.size == page.headers.size && row.none { it.length > 1_200 })
                    require(put(page.pageNumber to (index + 1), page.headers to row) == null) {
                        "Repeated source row position."
                    }
                }
            }
        }
        require(admitted.legs.all { (it.pageNumber to it.rowNumber) in cellByPosition })
        val credited = admitted.legs.filter { it.movementMinor > 0 }
            .fold(0L) { total, leg -> Math.addExact(total, leg.movementMinor) }
        val debited = admitted.legs.filter { it.movementMinor < 0 }
            .fold(0L) { total, leg -> Math.addExact(total, Math.negateExact(leg.movementMinor)) }
        require(credited == admitted.moneyInMinor && debited == admitted.moneyOutMinor)
        val sourceId = fingerprint(admitted, pages)
        val identityHash = MessageDigest.getInstance("SHA-256")
            .digest(admitted.accountIdentityHashMaterial.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val helper = StatementArchiveDb(app)
        try {
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                val exists = db.rawQuery("SELECT 1 FROM statement_sources_v2 WHERE id=? LIMIT 1", arrayOf(sourceId))
                    .use { it.moveToFirst() }
                if (exists) {
                    db.setTransactionSuccessful()
                    return SaveResult(false, sourceId)
                }
                db.insertOrThrow("statement_sources_v2", null, ContentValues().apply {
                    put("id", sourceId); put("account_id", admitted.accountId)
                    put("provider_key", admitted.profile.providerKey)
                    put("account_identity_hash", identityHash)
                    put("currency", admitted.profile.currency)
                    put("document_format", admitted.documentFormat)
                    put("imported_at", System.currentTimeMillis())
                    put("row_count", admitted.sourceRows)
                    put("first_at", admitted.legs.minOf { it.occurredAt })
                    put("last_at", admitted.legs.maxOf { it.occurredAt })
                    put("money_in_minor", admitted.moneyInMinor)
                    put("money_out_minor", admitted.moneyOutMinor)
                    put("totals_verified", 1)
                })
                admitted.legs.forEachIndexed { number, leg ->
                    val original = requireNotNull(cellByPosition[leg.pageNumber to leg.rowNumber])
                    db.insertOrThrow("statement_legs_v2", null, ContentValues().apply {
                        put("source_id", sourceId); put("leg_number", number)
                        put("page_number", leg.pageNumber); put("row_number", leg.rowNumber)
                        put("occurred_at", leg.occurredAt)
                        put("description", leg.description); put("reference", leg.reference)
                        put("movement_minor", leg.movementMinor)
                        put("running_balance_minor", leg.runningBalanceMinor)
                        put("source_cells_json", JSONObject().apply {
                            put("headers", JSONArray(original.first))
                            put("cells", JSONArray(original.second))
                        }.toString())
                    })
                }
                db.setTransactionSuccessful()
                return SaveResult(true, sourceId)
            } finally { db.endTransaction() }
        } finally { helper.close() }
    }

    fun sourcesFor(accountId: String): List<Source> {
        val helper = StatementArchiveDb(app)
        try {
            return helper.readableDatabase.rawQuery(
                """SELECT id,account_id,provider_key,currency,document_format,imported_at,first_at,last_at,
                    row_count,money_in_minor,money_out_minor FROM statement_sources_v2
                    WHERE account_id=? ORDER BY last_at DESC,id""".trimIndent(),
                arrayOf(accountId)
            ).use { c -> buildList {
                while (c.moveToNext()) add(Source(c.getString(0), c.getString(1), c.getString(2),
                    c.getString(3), c.getString(4), c.getLong(5), c.getLong(6), c.getLong(7),
                    c.getInt(8), c.getLong(9), c.getLong(10)))
            } }
        } finally { helper.close() }
    }

    fun legsFor(sourceId: String): List<Leg> {
        val helper = StatementArchiveDb(app)
        try {
            return helper.readableDatabase.rawQuery(
                """SELECT l.source_id,l.leg_number,l.page_number,l.row_number,l.occurred_at,
                    l.description,l.reference,l.movement_minor,l.running_balance_minor,l.source_cells_json,
                    c.category_id FROM statement_legs_v2 l
                    LEFT JOIN statement_category_labels_v2 c
                    ON c.source_id=l.source_id AND c.leg_number=l.leg_number
                    WHERE l.source_id=? ORDER BY l.leg_number""".trimIndent(), arrayOf(sourceId)
            ).use { c -> buildList {
                while (c.moveToNext()) {
                    val original = JSONObject(c.getString(9))
                    fun strings(key: String): List<String> = original.getJSONArray(key).let { arr ->
                        List(arr.length()) { index -> arr.getString(index) }
                    }
                    add(Leg(c.getString(0), c.getInt(1), c.getInt(2), c.getInt(3),
                        c.getLong(4), c.getString(5), if (c.isNull(6)) null else c.getString(6),
                        c.getLong(7), c.getLong(8), strings("headers"), strings("cells"),
                        if (c.isNull(10)) null else c.getString(10)))
                }
            } }
        } finally { helper.close() }
    }
}

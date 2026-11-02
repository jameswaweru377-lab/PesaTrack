package com.kedevjimmy.pesatrack.data.local

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic separate-PDF journal: the same rows intentionally do NOT exist in Activity. */
@RunWith(AndroidJUnit4::class)
class StatementArchiveReceivableSearchTest {
    private lateinit var context: Context
    private lateinit var archive: StatementActivityStore
    private val date = 1_790_000_000_000L
    private val batchId = "a".repeat(64)

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("pesatrack_statement_activity.db")
        archive = StatementActivityStore(context)
        archive.batches() // initialize the independent archive schema
        context.openOrCreateDatabase("pesatrack_statement_activity.db",Context.MODE_PRIVATE,null).use { db ->
            db.insertOrThrow("statement_batches",null,ContentValues().apply {
                put("id",batchId);put("account_id","synthetic-wallet")
                put("recorded_at",date);put("row_count",3)
                put("movement_count",2);put("informational_count",1)
                put("paid_in_minor",4000);put("paid_out_minor",12000)
                put("totals_verified",1)
            })
            val examples = listOf(
                Triple("Send Money to Sample Borrower 0712 000000",0L,12000L),
                Triple("Funds from Sample Borrower +254712000000",4000L,0L),
                Triple("Sample Borrower account information",0L,0L)
            )
            examples.forEachIndexed { index, triple ->
                db.insertOrThrow("statement_activity_lines",null,ContentValues().apply {
                    put("batch_id",batchId);put("line_number",index)
                    put("page_number",1);put("row_number",index+1)
                    put("reference","TX0000000" + index)
                    put("occurred_at",date-index*1000L)
                    put("description",triple.first)
                    put("paid_in_minor",triple.second)
                    put("withdrawn_minor",triple.third)
                    put("balance_minor",60000)
                })
            }
        }
    }

    @After fun tearDown() { context.deleteDatabase("pesatrack_statement_activity.db") }

    @Test fun archiveOnlyRowsAreSearchableWithoutCreatingLedgerTransactions() {
        val found = archive.searchForReceivables("Sample Borrower")
        assertEquals(2, found.rows.size)
        assertEquals(-12000L,found.rows.first().signedMinor)
        assertEquals(4000L,found.rows.last().signedMinor)
        assertTrue(found.rows.all { it.totalsVerified })
        assertNull(found.nextOffset)
    }

    @Test fun phoneAndDateSearchWorkWithPunctuationInStatements() {
        assertEquals(2,archive.searchForReceivables("0712000000").rows.size)
        assertEquals(2,archive.searchForReceivables("254712000000").rows.size)
        assertEquals(1,archive.searchForReceivables("Sample Borrower",
            fromEpochMs=date, throughEpochMs=date).rows.size)
        assertTrue(archive.searchForReceivables("Sample Borrower",
            fromEpochMs=date+1).rows.isEmpty())
    }

    @Test fun pagingAndLiteralWildcardEscapingStayBounded() {
        val first = archive.searchForReceivables("Sample Borrower",limit=1)
        assertEquals(1,first.rows.size)
        assertEquals(1,first.nextOffset)
        val second = archive.searchForReceivables("Sample Borrower",limit=1,offset=1)
        assertEquals(1,second.rows.size)
        assertTrue(first.rows.single().key != second.rows.single().key)
        assertNull(second.nextOffset)
        assertTrue(archive.searchForReceivables("%_").rows.isEmpty())
    }
    @Test fun onlyExactVerifiedUnmodifiedCashRowCanBePromoted() {
        val original = archive.searchForReceivables("Sample Borrower").rows.first()
        val checked = archive.verifiedMovementForActivity(original)
        assertEquals(original.description,checked.description)
        assertEquals(original.signedMinor,checked.signedMinor)
        assertEquals(original.reference,checked.reference)

        val stale = original.copy(description="Changed statement row")
        var rejected = false
        try { archive.verifiedMovementForActivity(stale) }
        catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
    }

    @Test fun unverifiedSavedPdfCannotPromoteMovementIntoActivity() {
        val original = archive.searchForReceivables("Sample Borrower").rows.first()
        context.openOrCreateDatabase("pesatrack_statement_activity.db",Context.MODE_PRIVATE,null).use { db ->
            db.update("statement_batches",ContentValues().apply { put("totals_verified",0) },
                "id=?",arrayOf(batchId))
        }
        var rejected = false
        try { archive.verifiedMovementForActivity(original.copy(totalsVerified=false)) }
        catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
    }

    @Test fun deletingOneApprovedArchiveCascadesRowsAndLabelsButRetainsSharedRules() {
        val chosen = archive.batches().single()
        context.openOrCreateDatabase("pesatrack_statement_activity.db",Context.MODE_PRIVATE,null).use { db ->
            db.insertOrThrow("statement_category_labels",null,ContentValues().apply {
                put("batch_id",batchId);put("page_number",1);put("row_number",1)
                put("category_id","FOOD")
            })
            db.insertOrThrow("statement_category_rules",null,ContentValues().apply {
                put("exact_merchant_key","fictional-shop");put("category_id","FOOD")
            })
        }
        assertTrue(archive.deleteSavedStatement(chosen))
        assertTrue(archive.batches().isEmpty())
        assertTrue(archive.searchForReceivables("Sample Borrower").rows.isEmpty())
        context.openOrCreateDatabase("pesatrack_statement_activity.db",Context.MODE_PRIVATE,null).use { db ->
            db.rawQuery("SELECT COUNT(*) FROM statement_activity_lines",null).use { c ->
                assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0))
            }
            db.rawQuery("SELECT COUNT(*) FROM statement_category_labels",null).use { c ->
                assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0))
            }
            db.rawQuery("SELECT COUNT(*) FROM statement_category_rules",null).use { c ->
                assertTrue(c.moveToFirst());assertEquals(1,c.getInt(0))
            }
        }
    }

    @Test fun staleArchiveDeletionIsRejectedWithoutChangingSavedStatement() {
        val current = archive.batches().single()
        assertFalse(archive.deleteSavedStatement(current.copy(rows=current.rows + 1)))
        assertEquals(current,archive.batches().single())
        assertEquals(2,archive.searchForReceivables("Sample Borrower").rows.size)
    }

    @Test fun v3ArchiveMigratesToChronologicalIndexWithoutChangingSavedMoneyOrLabels() {
        val before = archive.searchForReceivables("Sample Borrower").rows
        val existing = archive.batches().single()
        context.openOrCreateDatabase("pesatrack_statement_activity.db", Context.MODE_PRIVATE, null).use { db ->
            db.insertOrThrow("statement_category_labels", null, ContentValues().apply {
                put("batch_id", batchId);put("page_number", 1);put("row_number", 1)
                put("category_id", "FOOD")
            })
            // Model an existing installed v3 archive. The only v4 migration
            // must add the index, not rewrite the PDF rows or category labels.
            db.execSQL("DROP INDEX statement_lines_recent_global")
            db.version = 3
        }

        val after = archive.searchForReceivables("Sample Borrower").rows
        assertEquals(before, after)
        assertEquals(existing, archive.batches().single())

        context.openOrCreateDatabase("pesatrack_statement_activity.db", Context.MODE_PRIVATE, null).use { db ->
            assertEquals(StatementArchiveDb.VERSION, db.version)
            db.rawQuery("PRAGMA index_list('statement_activity_lines')", null).use { c ->
                var found = false
                while (c.moveToNext()) {
                    if (c.getString(1) == "statement_lines_recent_global") found = true
                }
                assertTrue("Global chronological index should survive the migration", found)
            }
            db.rawQuery("SELECT category_id FROM statement_category_labels WHERE batch_id=?", arrayOf(batchId)).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("FOOD", c.getString(0))
            }
        }
    }

    @Test fun chronologicalIndexSupportsQueryPlanWithoutChangingMatchOrdering() {
        val rows = archive.searchForReceivables("Sample Borrower", limit=1)
        assertEquals(1, rows.rows.size)
        assertEquals(-12000L, rows.rows.single().signedMinor)
        context.openOrCreateDatabase("pesatrack_statement_activity.db",Context.MODE_PRIVATE,null).use { db ->
            val query = """
                EXPLAIN QUERY PLAN
                SELECT s.batch_id,s.line_number FROM statement_activity_lines s
                JOIN statement_batches b ON b.id=s.batch_id
                WHERE (s.paid_in_minor > 0 OR s.withdrawn_minor > 0)
                    AND s.description LIKE ? ESCAPE '!'
                ORDER BY s.occurred_at DESC,s.batch_id,s.line_number LIMIT ? OFFSET ?
            """.trimIndent()
            db.rawQuery(query,arrayOf("%Sample%Borrower%","25","0")).use { c ->
                val plans = mutableListOf<String>()
                while (c.moveToNext()) plans += c.getString(3)
                assertTrue("PDF search should have an indexed chronological scan: $plans",
                    plans.any { it.contains("statement_lines_recent_global") })
            }
        }
    }


}

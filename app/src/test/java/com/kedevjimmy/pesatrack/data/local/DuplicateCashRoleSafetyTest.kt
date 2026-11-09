package com.kedevjimmy.pesatrack.data.local

import android.database.sqlite.SQLiteDatabase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Synthetic local database only; no phone, private messages or ledger changes. */
@RunWith(RobolectricTestRunner::class)
class DuplicateCashRoleSafetyTest {
    private val references = listOf(
        "debt_events" to "linked_transaction_id",
        "investment_events" to "linked_transaction_id",
        "personal_receivable_events" to "linked_transaction_id",
        "transaction_allocations" to "transaction_id",
        "payday_snapshots" to "salary_transaction_id",
        "transactions" to "related_transaction_id"
    )

    private fun syntheticDb(): SQLiteDatabase = SQLiteDatabase.create(null).apply {
        references.forEach { (table, column) ->
            execSQL("CREATE TABLE $table ($column INTEGER)")
        }
    }

    @Test fun unrelatedFinancialRelationshipsDoNotBlockTwoOrdinaryCashLegs() {
        syntheticDb().use { db ->
            references.forEach { (table, column) ->
                db.execSQL("INSERT INTO $table ($column) VALUES (99)")
            }
            assertFalse(DuplicateCashRoleSafety.hasDependents(db, 11L, 12L))
        }
    }

    @Test fun everyIndirectCashRoleBlocksDuplicateExclusionOnEitherSide() {
        syntheticDb().use { db ->
            references.forEach { (table, column) ->
                for (id in listOf(11L, 12L)) {
                    db.execSQL("INSERT INTO $table ($column) VALUES (?)", arrayOf(id))
                    assertTrue("$table linked to $id must block exclusion",
                        DuplicateCashRoleSafety.hasDependents(db, 11L, 12L))
                    db.delete(table, null, null)
                }
            }
        }
    }
}

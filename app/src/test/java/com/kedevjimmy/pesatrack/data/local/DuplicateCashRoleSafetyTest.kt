package com.kedevjimmy.pesatrack.data.local

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Synthetic local database only; no phone, private messages or ledger changes. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class DuplicateCashRoleSafetyTest {
    private lateinit var app: Context

    @Before fun setup() {
        app = RuntimeEnvironment.getApplication()
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
    }

    @After fun cleanup() {
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
    }

    private val references = listOf(
        "debt_events" to "linked_transaction_id",
        "fuliza_events" to "linked_transaction_id",
        "financial_events" to "linked_transaction_id",
        "investment_events" to "linked_transaction_id",
        "personal_receivable_events" to "linked_transaction_id",
        "transaction_allocations" to "transaction_id",
        "liability_payment_decisions" to "transaction_id",
        "pay_cycles" to "start_transaction_id",
        "cycle_protections" to "cycle_start_transaction_id",
        "payday_snapshots" to "salary_transaction_id",
        "transactions" to "related_transaction_id"
    )

    private fun syntheticDb(includeLegacyFuliza: Boolean = true): SQLiteDatabase = SQLiteDatabase.create(null).apply {
        references.forEach { (table, column) ->
            if (includeLegacyFuliza || table != "fuliza_events") {
                execSQL("CREATE TABLE $table ($column INTEGER)")
            }
        }
    }

    @Test fun freshInstallSchemaWithoutLegacyFulizaTableAllowsOrdinaryCashLegs() {
        val db = PesaTrackDatabase.get(app).writableDatabase
        assertFalse(DuplicateCashRoleSafety.hasDependents(db, 11L, 12L))
    }

    @Test fun olderInstallWithoutLegacyFulizaTableStillProtectsFinancialDependencies() {
        syntheticDb(includeLegacyFuliza = false).use { db ->
            assertFalse(DuplicateCashRoleSafety.hasDependents(db, 11L, 12L))
            db.execSQL("INSERT INTO financial_events (linked_transaction_id) VALUES (12)")
            assertTrue(DuplicateCashRoleSafety.hasDependents(db, 11L, 12L))
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

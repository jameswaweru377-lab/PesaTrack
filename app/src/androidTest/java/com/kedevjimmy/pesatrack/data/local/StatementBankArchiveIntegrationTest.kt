package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.statements.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic evidence only; never uses a real bank statement or identifier. */
@RunWith(AndroidJUnit4::class)
class StatementBankArchiveIntegrationTest {
    private lateinit var app: Context
    private lateinit var store: StatementBankArchiveStore
    private val headers = listOf("Date", "Value", "Particulars", "Money Out", "Money In", "Balance")
    private val pages = listOf(StatementTablePage(1, headers, listOf(
        listOf("01-09-2026", "", "Invented deposit", "", "50.00", "50.00 Cr"),
        listOf("02-09-2026", "", "Invented fee", "20.00", "", "30.00 Cr")
    )))

    @Before fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
        app.deleteDatabase("pesatrack_statement_activity.db")
        store = StatementBankArchiveStore(app)
    }

    @After fun tearDown() {
        PesaTrackDatabase.resetForTests()
        app.deleteDatabase("pesatrack.db")
        app.deleteDatabase("pesatrack_statement_activity.db")
    }

    private fun accepted(accountId: String): StatementAdmissionResult.Accepted {
        val profile = StatementSourceProfileRegistry.forProvider("KENYAN_BANK_EQUITY")!!
            .copy(sampleValidatedFormats = setOf("PDF"))
        val result = UniversalStatementTableInterpreter.interpret(
            pages, openingBalanceMinor = 0L, profile = profile.table
        ) as StatementTableResult.Accepted
        return StatementAdmissionResult.Accepted(
            accountId, "001234567890", profile, "PDF", 0L, result.legs, result.sourceRows,
            5_000L, 2_000L
        )
    }

    @Test fun requiresConfirmedAccountAndExactFullSourceIdentity() {
        val unconfirmed = AccountStore.get(app).save("Invented Equity", AccountType.BANK)
        AccountStore.get(app).updateDetails(unconfirmed.id, "001234567890", null)
        assertNotNull(runCatching { store.save(accepted(unconfirmed.id), pages) }.exceptionOrNull())
        assertTrue(store.sourcesFor(unconfirmed.id).isEmpty())

        AccountOwnershipConsentStore(app).recordConsent(unconfirmed.id, true)
        AccountStore.get(app).updateDetails(unconfirmed.id, "001234567891", null)
        assertNotNull(runCatching { store.save(accepted(unconfirmed.id), pages) }.exceptionOrNull())
        assertTrue(store.sourcesFor(unconfirmed.id).isEmpty())
    }

    @Test fun atomicIdempotentBankArchiveNeverCreatesActivity() {
        val bank = AccountOwnershipConsentStore(app).createOwnedAccount("Invented Equity", AccountType.BANK)
        AccountStore.get(app).updateDetails(bank.id, "001234567890", null)
        val first = store.save(accepted(bank.id), pages)
        assertTrue(first.added)
        val again = store.save(accepted(bank.id), pages)
        assertFalse(again.added)
        assertEquals(first.sourceId, again.sourceId)
        assertEquals(1, store.sourcesFor(bank.id).size)
        val rows = store.legsFor(first.sourceId)
        assertEquals(2, rows.size)
        assertEquals(-2_000L, rows[1].movementMinor)
        assertEquals("Money Out", rows[1].originalHeaders[3])
        assertEquals("20.00", rows[1].originalCells[3])
        val ledger = PesaTrackDatabase.get(app).readableDatabase
        assertEquals(0L, ledger.compileStatement("SELECT COUNT(*) FROM transactions").simpleQueryForLong())
    }
}

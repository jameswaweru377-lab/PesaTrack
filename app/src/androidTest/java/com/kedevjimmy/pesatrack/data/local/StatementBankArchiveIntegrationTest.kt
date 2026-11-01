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

    private fun accepted(
        accountId: String, sourcePages: List<StatementTablePage> = pages
    ): StatementAdmissionResult.Accepted {
        val profile = StatementSourceProfileRegistry.forProvider("KENYAN_BANK_EQUITY")!!
            .copy(sampleValidatedFormats = setOf("PDF"))
        val result = UniversalStatementTableInterpreter.interpret(
            sourcePages, openingBalanceMinor = 0L, profile = profile.table
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

    @Test fun localBankCategoryLabelsAreReversibleAndCannotReclassifyActivity() {
        val bank = AccountOwnershipConsentStore(app).createOwnedAccount(
            "Invented Equity", AccountType.BANK)
        AccountStore.get(app).updateDetails(bank.id, "001234567890", null)
        val original = store.save(accepted(bank.id), pages)
        val sourceBefore = store.sourcesFor(bank.id).single()
        val bankRowsBefore = store.legsFor(original.sourceId)

        store.labelLeg(original.sourceId, 1, "GROCERIES")
        store.labelLeg(original.sourceId, 0, "INCOME")
        assertEquals("GROCERIES", store.legsFor(original.sourceId)[1].categoryId)
        assertEquals("INCOME", store.legsFor(original.sourceId)[0].categoryId)
        assertNotNull(runCatching {
            store.labelLeg(original.sourceId, 1, "INCOME")
        }.exceptionOrNull())
        assertNotNull(runCatching {
            store.labelLeg(original.sourceId, 0, "TRANSFER")
        }.exceptionOrNull())
        assertNotNull(runCatching {
            store.labelLeg("0".repeat(64), 1, "GROCERIES")
        }.exceptionOrNull())
        assertEquals("GROCERIES", store.legsFor(original.sourceId)[1].categoryId)

        val alternativePages = pages.map { page ->
            page.copy(rows = page.rows.mapIndexed { index, cells ->
                if (index == 1) cells.toMutableList().apply { this[2] = "Another bank copy" }
                else cells
            })
        }
        val alternative = store.save(accepted(bank.id, alternativePages), alternativePages)
        assertTrue(alternative.added)
        assertNull(store.legsFor(alternative.sourceId)[1].categoryId)

        assertFalse(store.save(accepted(bank.id), pages).added)
        assertEquals("GROCERIES", store.legsFor(original.sourceId)[1].categoryId)
        store.labelLeg(original.sourceId, 1, null)
        assertNull(store.legsFor(original.sourceId)[1].categoryId)
        assertEquals(sourceBefore, store.sourcesFor(bank.id).first { it.id == original.sourceId })
        val rowsAfter = store.legsFor(original.sourceId)
        bankRowsBefore.zip(rowsAfter).forEach { (before, after) ->
            assertEquals(before.copy(categoryId = after.categoryId), after)
        }
        assertEquals(0L, PesaTrackDatabase.get(app).readableDatabase
            .compileStatement("SELECT COUNT(*) FROM transactions").simpleQueryForLong())
    }

    @Test fun limitedLegacyArchiveIsSeparateFromVerifiedReportsAndIdempotent() {
        val bank = AccountOwnershipConsentStore(app).createOwnedAccount(
            "Invented Equity", AccountType.BANK)
        AccountStore.get(app).updateDetails(bank.id, "001234567890", null)
        val profile = StatementSourceProfileRegistry.forProvider("KENYAN_BANK_EQUITY")!!
        val legacyPages = listOf(StatementTablePage(1,
            listOf("Transaction Details", "Payment reference", "Date", "Credit", "Debit", "Balance"),
            listOf(
                listOf("Invented deposit", "EXAMPLE00001", "01/09/2026", "100.00", "", "100.00"),
                listOf("Invented payment", "EXAMPLE00002", "02/09/2026", "", "20.00", "80.00")
            )))
        val admitted = StatementBankAdmission.verify(
            profile, AccountStore.get(app).listAccounts().first { it.id == bank.id },
            setOf(bank.id), profile.providerKey,
            StatementDocumentProof(profile.providerKey, "001234567890", "KES", "PDF",
                true, 1, emptyMap(), StatementPrintedTotal(2_000L, 10_000L), null,
                StatementDocumentAssurance.GRAND_TOTAL_AND_CHAIN_ONLY),
            legacyPages
        ) as StatementAdmissionResult.Accepted
        val saved = store.save(admitted, legacyPages)
        assertTrue(saved.added)
        assertFalse(store.save(admitted, legacyPages).added)
        assertTrue(store.sourcesFor(bank.id).isEmpty())
        assertEquals(1, store.limitedSourcesFor(bank.id).size)
        assertEquals(2, store.limitedLegsFor(saved.sourceId).size)
        assertEquals(0L, PesaTrackDatabase.get(app).readableDatabase
            .compileStatement("SELECT COUNT(*) FROM transactions").simpleQueryForLong())
    }

    @Test fun verifiedReimportSupersedesVisibleLimitedCopyWithoutDeletingItsEvidence() {
        val bank = AccountOwnershipConsentStore(app).createOwnedAccount(
            "Invented Equity", AccountType.BANK)
        AccountStore.get(app).updateDetails(bank.id, "001234567890", null)
        val profile = StatementSourceProfileRegistry.forProvider("KENYAN_BANK_EQUITY")!!
        val syntheticPages = listOf(StatementTablePage(1,
            listOf("Transaction Details", "Payment reference", "Date", "Credit", "Debit", "Balance"),
            listOf(
                listOf("Invented deposit", "EXAMPLE00001", "01/09/2026", "100.00", "", "100.57"),
                listOf("Invented payment", "EXAMPLE00002", "02/09/2026", "", "20.00", "80.57")
            )))
        val owned = AccountStore.get(app).listAccounts().first { it.id == bank.id }
        val limitedProof = StatementDocumentProof(profile.providerKey, "001234567890", "KES", "PDF",
            true, 1, emptyMap(), StatementPrintedTotal(2_000L, 10_000L), null,
            StatementDocumentAssurance.GRAND_TOTAL_AND_CHAIN_ONLY)
        val limited = StatementBankAdmission.verify(profile, owned, setOf(bank.id),
            profile.providerKey, limitedProof, syntheticPages) as StatementAdmissionResult.Accepted
        val savedLimited = store.save(limited, syntheticPages)
        assertEquals(1, store.limitedSourcesFor(bank.id).size)
        assertTrue(store.sourcesFor(bank.id).isEmpty())

        val newlyVerified = StatementBankAdmission.verify(profile, owned, setOf(bank.id),
            profile.providerKey, limitedProof.copy(
                assurance = StatementDocumentAssurance.VERIFIED_DERIVED_OPENING,
                printedClosingBalanceMinor = 8_057L
            ), syntheticPages) as StatementAdmissionResult.Accepted
        val savedVerified = store.save(newlyVerified, syntheticPages)
        assertTrue(savedVerified.added)
        assertEquals(savedLimited.sourceId, savedVerified.sourceId)
        assertEquals(StatementBankAdmission.DERIVED_OPENING_ARCHIVE_FORMAT,
            store.sourcesFor(bank.id).single().format)
        assertTrue(store.limitedSourcesFor(bank.id).isEmpty())
        assertEquals(2, store.limitedLegsFor(savedLimited.sourceId).size)
        val months = StatementBankReportReader(app).monthsFor(bank.id)
        assertEquals(1, months.size)
        assertEquals(10_000L, months.single().moneyInMinor)
        assertEquals(2_000L, months.single().moneyOutMinor)
        assertFalse(store.save(newlyVerified, syntheticPages).added)
        assertEquals(0L, PesaTrackDatabase.get(app).readableDatabase
            .compileStatement("SELECT COUNT(*) FROM transactions").simpleQueryForLong())
    }
}

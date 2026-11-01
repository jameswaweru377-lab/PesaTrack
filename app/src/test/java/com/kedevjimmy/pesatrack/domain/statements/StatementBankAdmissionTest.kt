package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import org.junit.Assert.*
import org.junit.Test

/** All source identities, dates, descriptions and money are invented. */
class StatementBankAdmissionTest {
    private val profile = StatementSourceProfileRegistry.forProvider("KENYAN_BANK_EQUITY")!!
        .copy(sampleValidatedFormats = setOf("PDF"))
    private val account = MoneyAccount("bank-1", "My invented bank", AccountType.BANK,
        identifier = "001234567890")
    private val document = StatementDocumentProof(
        detectedProviderKey = "KENYAN_BANK_EQUITY", detectedAccountIdentifier = "001234567890",
        currency = "KES", format = "PDF", allSourceRegionsAccountedFor = true,
        pages = 1, printedPageTotals = mapOf(1 to StatementPrintedTotal(2_000, 5_000)),
        printedGrandTotal = StatementPrintedTotal(2_000, 5_000), openingBalanceMinor = 0L
    )
    private val pages = listOf(StatementTablePage(1,
        listOf("Date", "Value", "Particulars", "Money Out", "Money In", "Balance"),
        listOf(
            listOf("01-09-2026", "", "Invented deposit", "", "50.00", "50.00 Cr"),
            listOf("02-09-2026", "", "Invented charge", "20.00", "", "30.00 Cr")
        )))

    private fun admit(
        selected: MoneyAccount = account,
        owned: Set<String> = setOf(account.id),
        provider: String? = profile.providerKey,
        proof: StatementDocumentProof = document,
        sourcePages: List<StatementTablePage> = pages,
        source: StatementSourceProfile? = profile
    ) = StatementBankAdmission.verify(source, selected, owned, provider, proof, sourcePages)

    @Test fun acceptsOnlyFullyProvenSyntheticSource() {
        val accepted = admit() as StatementAdmissionResult.Accepted
        assertEquals(5_000L, accepted.moneyInMinor)
        assertEquals(2_000L, accepted.moneyOutMinor)
        assertEquals("bank-1", accepted.accountId)
    }

    @Test fun unknownProviderAndNotOwnedAccountCannotPass() {
        assertEquals(StatementAdmissionResult.Reason.UNSUPPORTED_PROVIDER,
            (admit(source = null) as StatementAdmissionResult.Blocked).reason)
        assertEquals(StatementAdmissionResult.Reason.OWNERSHIP_UNVERIFIED,
            (admit(owned = emptySet()) as StatementAdmissionResult.Blocked).reason)
        assertEquals(StatementAdmissionResult.Reason.WRONG_ACCOUNT,
            (admit(provider = "KENYAN_BANK_KCB") as StatementAdmissionResult.Blocked).reason)
    }

    @Test fun maskedAndWrongSourceAccountsNeverMatch() {
        assertEquals(StatementAdmissionResult.Reason.UNKNOWN_ACCOUNT_IDENTITY,
            (admit(proof = document.copy(detectedAccountIdentifier = "****7890"))
                as StatementAdmissionResult.Blocked).reason)
        assertEquals(StatementAdmissionResult.Reason.WRONG_ACCOUNT,
            (admit(proof = document.copy(detectedAccountIdentifier = "009999999999"))
                as StatementAdmissionResult.Blocked).reason)
    }

    @Test fun unverifiedFormatCannotBeEnabledByChoosingAnAccount() {
        assertEquals(StatementAdmissionResult.Reason.UNVERIFIED_FORMAT,
            (admit(source = StatementSourceProfileRegistry.forProvider("KENYAN_BANK_EQUITY")!!
                .copy(sampleValidatedFormats = emptySet()))
                as StatementAdmissionResult.Blocked).reason)
    }

    @Test fun incompleteSourceAndContradictoryTotalsBlockSave() {
        assertEquals(StatementAdmissionResult.Reason.INCOMPLETE_DOCUMENT,
            (admit(proof = document.copy(allSourceRegionsAccountedFor = false))
                as StatementAdmissionResult.Blocked).reason)
        assertEquals(StatementAdmissionResult.Reason.TOTAL_MISMATCH,
            (admit(proof = document.copy(printedGrandTotal = StatementPrintedTotal(2_500, 5_000)))
                as StatementAdmissionResult.Blocked).reason)
        assertEquals(StatementAdmissionResult.Reason.MISSING_PRINTED_TOTALS,
            (admit(proof = document.copy(printedPageTotals = emptyMap()))
                as StatementAdmissionResult.Blocked).reason)
    }

    @Test fun missingBalanceAnchorBlocksEvenWhenPrintedTotalsMatch() {
        assertEquals(StatementAdmissionResult.Reason.UNVERIFIED_BALANCE,
            (admit(proof = document.copy(openingBalanceMinor = null))
                as StatementAdmissionResult.Blocked).reason)
    }

    @Test fun coverPageMayHaveNoTransactionsOrPrintedTotals() {
        val onSecondPage = pages.map { it.copy(pageNumber = 2) }
        val result = admit(
            proof = document.copy(pages = 2,
                printedPageTotals = mapOf(2 to StatementPrintedTotal(2_000, 5_000))),
            sourcePages = onSecondPage
        )
        assertTrue(result.toString(), result is StatementAdmissionResult.Accepted)
    }

    @Test fun printedGrandTotalAndCompleteChainPermitOnlyLimitedAssurance() {
        val limited = document.copy(
            pages = 1,
            assurance = StatementDocumentAssurance.GRAND_TOTAL_AND_CHAIN_ONLY,
            openingBalanceMinor = null,
            printedPageTotals = emptyMap()
        )
        val accepted = admit(proof = limited) as StatementAdmissionResult.Accepted
        assertEquals(StatementDocumentAssurance.GRAND_TOTAL_AND_CHAIN_ONLY, accepted.assurance)
        assertNull(accepted.openingBalanceMinor)
        assertEquals(5_000L, accepted.moneyInMinor)
        assertEquals(2_000L, accepted.moneyOutMinor)
    }

    @Test fun limitedAssuranceDoesNotBypassIssuerCoverageOrGrandTotals() {
        val limited = document.copy(
            assurance = StatementDocumentAssurance.GRAND_TOTAL_AND_CHAIN_ONLY,
            openingBalanceMinor = null,
            printedPageTotals = emptyMap()
        )
        assertEquals(StatementAdmissionResult.Reason.INCOMPLETE_DOCUMENT,
            (admit(proof = limited.copy(allSourceRegionsAccountedFor = false))
                as StatementAdmissionResult.Blocked).reason)
        assertEquals(StatementAdmissionResult.Reason.TOTAL_MISMATCH,
            (admit(proof = limited.copy(printedGrandTotal = StatementPrintedTotal(2_001, 5_000)))
                as StatementAdmissionResult.Blocked).reason)
        assertEquals(StatementAdmissionResult.Reason.INCOMPLETE_DOCUMENT,
            (admit(proof = limited.copy(pages = 2)) as StatementAdmissionResult.Blocked).reason)
        assertEquals(StatementAdmissionResult.Reason.WRONG_ACCOUNT,
            (admit(proof = limited.copy(detectedProviderKey = "KENYAN_BANK_KCB"))
                as StatementAdmissionResult.Blocked).reason)
    }

    private val derivedPages get() = pages.map { page -> page.copy(rows = listOf(
        listOf("01-09-2026", "", "Invented deposit", "", "50.00", "50.57 Cr"),
        listOf("02-09-2026", "", "Invented charge", "20.00", "", "30.57 Cr")
    )) }

    private val derivedProof get() = document.copy(
        assurance = StatementDocumentAssurance.VERIFIED_DERIVED_OPENING,
        openingBalanceMinor = null, printedPageTotals = emptyMap(),
        printedClosingBalanceMinor = 3_057L
    )

    @Test fun printedClosingAndBothIndependentOpeningDerivationsAdmitVerifiedEquity() {
        val accepted = admit(proof = derivedProof, sourcePages = derivedPages)
            as StatementAdmissionResult.Accepted
        assertEquals(StatementDocumentAssurance.VERIFIED_DERIVED_OPENING, accepted.assurance)
        assertEquals(57L, accepted.openingBalanceMinor)
        assertEquals(StatementBankAdmission.DERIVED_OPENING_ARCHIVE_FORMAT, accepted.documentFormat)
        assertEquals(5_000L, accepted.moneyInMinor)
        assertEquals(2_000L, accepted.moneyOutMinor)
    }

    @Test fun derivedOpeningVerificationFailsClosedForMissingOrContradictoryEvidence() {
        assertEquals(StatementAdmissionResult.Reason.MISSING_PRINTED_TOTALS,
            (admit(proof = derivedProof.copy(printedClosingBalanceMinor = null),
                sourcePages = derivedPages) as StatementAdmissionResult.Blocked).reason)
        assertEquals(StatementAdmissionResult.Reason.UNVERIFIED_BALANCE,
            (admit(proof = derivedProof.copy(printedClosingBalanceMinor = 3_058L),
                sourcePages = derivedPages) as StatementAdmissionResult.Blocked).reason)
        assertEquals(StatementAdmissionResult.Reason.INCOMPLETE_DOCUMENT,
            (admit(proof = derivedProof.copy(pages = 2),
                sourcePages = derivedPages) as StatementAdmissionResult.Blocked).reason)
        assertEquals(StatementAdmissionResult.Reason.INCOMPLETE_DOCUMENT,
            (admit(proof = derivedProof.copy(printedPageTotals = document.printedPageTotals),
                sourcePages = derivedPages) as StatementAdmissionResult.Blocked).reason)
        assertEquals(StatementAdmissionResult.Reason.INCOMPLETE_DOCUMENT,
            (admit(proof = derivedProof.copy(openingBalanceMinor = 57L),
                sourcePages = derivedPages) as StatementAdmissionResult.Blocked).reason)
        assertEquals(StatementAdmissionResult.Reason.TOTAL_MISMATCH,
            (admit(proof = derivedProof.copy(printedGrandTotal = StatementPrintedTotal(2_001, 5_000)),
                sourcePages = derivedPages) as StatementAdmissionResult.Blocked).reason)
    }
}

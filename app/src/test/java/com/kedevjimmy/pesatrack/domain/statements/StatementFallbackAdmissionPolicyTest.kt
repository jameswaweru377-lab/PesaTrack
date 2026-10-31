package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount
import org.junit.Assert.*
import org.junit.Test

class StatementFallbackAdmissionPolicyTest {
    private val profile = StatementSourceProfileRegistry.forProvider("KENYAN_BANK_EQUITY")!!
    private val account = MoneyAccount("bank-1", "Invented bank", AccountType.BANK,
        identifier = "001234567890")
    private val pages = listOf(StatementTablePage(1,
        listOf("Date", "Value", "Particulars", "Money Out", "Money In", "Balance"),
        listOf(
            listOf("01-09-2026", "", "Invented deposit", "", "50.00", "50.00 Cr"),
            listOf("02-09-2026", "", "Invented charge", "20.00", "", "30.00 Cr")
        )))
    private val legs = (UniversalStatementTableInterpreter.interpret(
        pages, openingBalanceMinor = 0L, profile = profile.table
    ) as StatementTableResult.Accepted).legs
    private val candidate = StatementProbeResult.Provisional(pages, legs, 2, false)
    private val proof = StatementDocumentProof(
        detectedProviderKey = profile.providerKey,
        detectedAccountIdentifier = "001234567890", currency = "KES", format = "PDF",
        allSourceRegionsAccountedFor = true, pages = 1,
        printedPageTotals = mapOf(1 to StatementPrintedTotal(2_000, 5_000)),
        printedGrandTotal = StatementPrintedTotal(2_000, 5_000), openingBalanceMinor = 0L
    )

    private fun verify(
        probe: StatementProbeResult = candidate,
        document: StatementDocumentProof? = proof,
        source: StatementSourceProfile? = profile,
        selected: MoneyAccount = account
    ) = StatementFallbackAdmissionPolicy.verify(
        probe, document, source, selected, setOf(account.id), profile.providerKey)

    private fun reason(result: StatementAdmissionResult) =
        (result as StatementAdmissionResult.Blocked).reason

    @Test fun noProofNeverAdmitsEvenPerfectBalances() {
        assertEquals(StatementAdmissionResult.Reason.INCOMPLETE_DOCUMENT, reason(verify(document = null)))
    }

    @Test fun unknownIssuerNeverAdmits() {
        assertEquals(StatementAdmissionResult.Reason.UNSUPPORTED_PROVIDER, reason(verify(source = null)))
    }

    @Test fun wrongAccountNeverAdmits() {
        assertEquals(StatementAdmissionResult.Reason.WRONG_ACCOUNT, reason(verify(
            selected = account.copy(identifier = "001111111111"))))
    }

    @Test fun oneMissingPrintedPageTotalNeverAdmits() {
        assertEquals(StatementAdmissionResult.Reason.MISSING_PRINTED_TOTALS,
            reason(verify(document = proof.copy(printedPageTotals = emptyMap()))))
    }

    @Test fun limitedAssuranceNeverAutoAdmitsThroughGenericFallback() {
        val limited = proof.copy(
            assurance = StatementDocumentAssurance.GRAND_TOTAL_AND_CHAIN_ONLY,
            openingBalanceMinor = null, printedPageTotals = emptyMap()
        )
        assertEquals(StatementAdmissionResult.Reason.INCOMPLETE_DOCUMENT,
            reason(verify(document = limited)))
    }

    @Test fun completeIndependentProofDelegatesToBankAdmission() {
        val accepted = verify() as StatementAdmissionResult.Accepted
        assertEquals(5_000L, accepted.moneyInMinor)
        assertEquals(2_000L, accepted.moneyOutMinor)
        assertEquals(2, accepted.sourceRows)
    }

    @Test fun independentlyPrintedOpeningAnchorsPreviouslyUnanchoredNativeProbe() {
        val withoutOpening = UniversalStatementTableInterpreter.interpret(
            pages, openingBalanceMinor = null, profile = profile.table
        ) as StatementTableResult.Accepted
        assertTrue(withoutOpening.oldestLegUnanchored)
        val live = candidate.copy(
            legs = withoutOpening.legs,
            sourceRows = withoutOpening.sourceRows,
            oldestLegUnanchored = withoutOpening.oldestLegUnanchored
        )
        val accepted = verify(probe = live) as StatementAdmissionResult.Accepted
        assertEquals(StatementDocumentAssurance.FULL, accepted.assurance)
    }

    @Test fun mutatedProbeTablesAfterProofRefuses() {
        val replaced = candidate.copy(legs = candidate.legs.dropLast(1))
        assertEquals(StatementAdmissionResult.Reason.UNVERIFIED_BALANCE,
            reason(verify(probe = replaced)))
    }

    @Test fun blockedProvisionalCandidateNeverAdmits() {
        assertEquals(StatementAdmissionResult.Reason.INCOMPLETE_DOCUMENT,
            reason(verify(probe = StatementProbeResult.Blocked(1, StatementProbeReason.UNRECOGNIZED_TABLE))))
    }
}

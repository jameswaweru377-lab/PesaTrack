package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialIntelligenceV2SemanticTest {
    @Test
    fun semanticCorpusProducesExpectedIntentAndRole() {
        FinancialIntelligenceV2SemanticCorpus.all
            .forEach { fixture ->
                val normalized =
                    FinancialMessageNormalizer.normalize(
                        title = fixture.title,
                        body = fixture.body
                    )
                val relevance =
                    FinancialMessageRelevanceGate
                        .assess(normalized)

                assertTrue(
                    fixture.id +
                        " should pass relevance first",
                    relevance.isRelevant
                )

                val semantic =
                    FinancialSemanticAnalyzer
                        .analyze(normalized)

                assertEquals(
                    fixture.id +
                        " primary intent mismatch",
                    fixture.expectedIntent,
                    semantic.primaryIntent
                )
                assertEquals(
                    fixture.id +
                        " role mismatch",
                    fixture.expectedRole,
                    semantic.role
                )
                assertEquals(
                    fixture.id +
                        " reference mismatch",
                    fixture.expectedReferences,
                    semantic.references
                        .map { it.value }
                        .toSet()
                )
            }
    }

    @Test
    fun semanticLayerExtractsMultipleProviderReferencesWithoutProviderRules() {
        val normalized =
            FinancialMessageNormalizer.normalize(
                title = "Example Bank",
                body =
                    "Your transaction of KES 5,000 " +
                        "has been credited to SAMPLE BENEFICIARY. " +
                        "Bank Ref ABC12345. " +
                        "M-Pesa Ref UFT12345."
            )

        val semantic =
            FinancialSemanticAnalyzer.analyze(
                normalized
            )

        assertEquals(
            FinancialSemanticIntent.TRANSFER,
            semantic.primaryIntent
        )
        assertEquals(
            setOf("ABC12345", "UFT12345"),
            semantic.references
                .map { it.value }
                .toSet()
        )
    }

    @Test
    fun semanticLoanBalanceCanCreateObservedLiabilityWithoutDrawdown() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "M-Shwari",
                body =
                    "Your total loan balance is Kshs 1,075.00."
            )

        assertTrue(interpretation.recognized)
        assertTrue(
            interpretation
                .semanticallyUnderstood
        )

        val balance =
            interpretation.balances.single()

        assertEquals(
            107_500L,
            balance.balanceMinor
        )
        assertEquals(
            FinancialAccountNature.LIABILITY,
            balance.account.nature
        )
        assertEquals(
            "NCBA_MSHWARI_LOAN",
            balance.account.providerKey
        )
    }

    @Test
    fun semanticLoanDisbursementCanCreateDebtFactForUnknownLender() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "Future Finance",
                body =
                    "Your loan of Kenya Shillings 8,000 " +
                        "has been disbursed to your Mobile Wallet account."
            )

        val event =
            interpretation.events.single() as
                DebtDrawdownEventFact

        assertEquals(800_000L, event.amountMinor)
        assertEquals(
            FinancialAccountNature.LIABILITY,
            event.liability.nature
        )
        assertEquals(null, event.liability.providerKey)
        assertEquals(
            com.kedevjimmy.pesatrack.domain.model
                .AccountType.MOBILE_WALLET,
            event.proceedsAccount
                ?.accountTypeHint
        )
        assertEquals(
            null,
            event.proceedsAccount?.providerKey
        )
    }

    @Test
    fun confirmationOnlyMessageNeverCreatesSecondLedgerEvent() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "Future Finance",
                body =
                    "Your transfer of funds has been processed."
            )

        assertFalse(interpretation.recognized)
        assertTrue(
            interpretation.semanticallyUnderstood
        )
        assertTrue(interpretation.events.isEmpty())
        assertEquals(
            FinancialSemanticRole.CONFIRMATION,
            interpretation
                .semanticAssessment
                ?.role
        )
    }

    @Test
    fun investmentOrderIsUnderstoodWithoutPretendingSharesWereBought() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "Broker",
                body =
                    "Your buy order of 10 SAMPLE shares " +
                        "valued at KES 367 has been submitted."
            )

        assertFalse(interpretation.recognized)
        assertTrue(
            interpretation.semanticallyUnderstood
        )
        assertTrue(interpretation.events.isEmpty())
        assertEquals(
            FinancialSemanticIntent.INVESTMENT_ORDER,
            interpretation
                .semanticAssessment
                ?.primaryIntent
        )
        assertEquals(
            FinancialSemanticRole.PENDING_ACTION,
            interpretation
                .semanticAssessment
                ?.role
        )
    }
}

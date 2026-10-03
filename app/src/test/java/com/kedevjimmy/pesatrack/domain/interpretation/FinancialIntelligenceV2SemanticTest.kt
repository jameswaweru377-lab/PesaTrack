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
    fun failedActionVetoesOtherwiseParseableBankTransfer() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "Future Bank",
                body =
                    "Your account has been debited KES 500.00 for transfer to SAMPLE PERSON on 1/10/2026. Transaction failed due to insufficient funds."
            )

        assertEquals(
            FinancialSemanticIntent.FAILED_ACTION,
            interpretation
                .semanticAssessment
                ?.primaryIntent
        )
        assertTrue(
            interpretation.events.isEmpty()
        )
        assertTrue(
            interpretation
                .semanticallyUnderstood
        )
    }

    @Test
    fun declinedRepaymentNeverCreatesLedgerMovementButKeepsObservedBalance() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "KCB M-PESA",
                body =
                    "Dear SAMPLE CUSTOMER, your loan repayment of KES 100.00 has been declined due to insufficient funds. Your KCB M-PESA Account balance is KES 0.00."
            )

        assertTrue(
            interpretation
                .semanticallyUnderstood
        )
        assertEquals(
            FinancialSemanticIntent.FAILED_ACTION,
            interpretation
                .semanticAssessment
                ?.primaryIntent
        )
        assertTrue(
            interpretation.events.isEmpty()
        )
        assertEquals(
            0L,
            interpretation.balances
                .single()
                .balanceMinor
        )
        assertEquals(
            "KCB_MPESA_SAVINGS",
            interpretation.balances
                .single()
                .account
                .providerKey
        )
    }

    @Test
    fun pendingTransferIsPendingActionAndNeverCreatesLedgerMovement() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "My Bank",
                    body =
                        "KES 1,000.00 transferred from Savings account to Current account pending."
                )

        assertTrue(
            interpretation.events.isEmpty()
        )
        assertEquals(
            FinancialSemanticIntent.TRANSFER,
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

    @Test
    fun pendingBankCreditKeepsObservedBalanceButNoMovement() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "My Bank",
                    body =
                        "KES 825.50 has been credited to your account XX9087 pending. Available balance KES 4,925.50."
                )

        assertTrue(
            interpretation.events.isEmpty()
        )
        assertEquals(
            FinancialSemanticIntent.RECEIPT,
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
        assertEquals(
            492_550L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun informationalBalanceMentioningNoPendingTransactionsStaysInformation() {
        val interpretation =
            FinancialMessageInterpreter
                .interpret(
                    title = "My Bank",
                    body =
                        "You have no pending transactions. Available balance KES 4,925.50."
                )

        assertTrue(
            interpretation.events.isEmpty()
        )
        assertEquals(
            FinancialSemanticIntent
                .BALANCE_UPDATE,
            interpretation
                .semanticAssessment
                ?.primaryIntent
        )
        assertEquals(
            FinancialSemanticRole.INFORMATION,
            interpretation
                .semanticAssessment
                ?.role
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
    fun executedInvestmentExtractsSettlementFacts() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "Broker",
                body =
                    "You have successfully bought 3 EQA shares " +
                        "valued at KES 3,021 via code UFT12345."
            )

        val investment =
            interpretation
                .semanticAssessment
                ?.investment
                ?: error(
                    "Expected investment execution fact."
                )

        assertEquals(
            SemanticInvestmentStage.EXECUTED,
            investment.stage
        )
        assertEquals(
            SemanticInvestmentAction.BUY,
            investment.action
        )
        assertEquals(
            3_000_000L,
            investment.quantityMicrounits
        )
        assertEquals(
            "EQA",
            investment.instrumentLabel
        )
        assertEquals(
            "instrument:EQA",
            investment.instrumentKey
        )
        assertEquals(
            302_100L,
            investment.valueMinor
        )
    }

    @Test
    fun submittedInvestmentOrderExtractsPendingFactsOnly() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "Broker",
                body =
                    "Your buy order of 10 SAMPLE shares " +
                        "valued at KES 367 has been submitted."
            )

        val investment =
            interpretation
                .semanticAssessment
                ?.investment
                ?: error(
                    "Expected pending investment fact."
                )

        assertEquals(
            SemanticInvestmentStage.ORDER_SUBMITTED,
            investment.stage
        )
        assertEquals(
            SemanticInvestmentAction.BUY,
            investment.action
        )
        assertEquals(
            10_000_000L,
            investment.quantityMicrounits
        )
        assertEquals(
            "SAMPLE",
            investment.instrumentLabel
        )
        assertEquals(
            36_700L,
            investment.valueMinor
        )
        assertFalse(interpretation.recognized)
    }

    @Test
    fun executedInvestmentSaleExtractsSellDelta() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "Broker",
                body =
                    "You successfully sold 2.5 ACME shares " +
                        "valued at KES 1,250.00 via code ABC12345."
            )

        val investment =
            interpretation
                .semanticAssessment
                ?.investment
                ?: error(
                    "Expected investment sale fact."
                )

        assertEquals(
            SemanticInvestmentAction.SELL,
            investment.action
        )
        assertEquals(
            SemanticInvestmentStage.EXECUTED,
            investment.stage
        )
        assertEquals(
            2_500_000L,
            investment.quantityMicrounits
        )
        assertEquals(
            "ACME",
            investment.instrumentLabel
        )
        assertEquals(
            125_000L,
            investment.valueMinor
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

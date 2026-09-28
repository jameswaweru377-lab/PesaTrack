package com.kedevjimmy.pesatrack.domain.interpretation

import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialInterpretationLearningTest {
    private val learnedPesaNova =
        FinancialAccountReference(
            entityKey = "discovered:pesanova",
            displayName = "PesaNova",
            providerKey = null,
            accountTypeHint =
                AccountType.MOBILE_WALLET,
            ownershipHint =
                AccountOwnershipHint.USER_OWNED,
            nature =
                FinancialAccountNature.ASSET
        )

    @Test
    fun confirmedUnknownAccountIsUnderstoodOnFutureMessages() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "PesaNova",
                body =
                    "TST12LRN01 Confirmed. Ksh 640.00 sent to SAMPLE PERSON 0712000000 on 28/9/26 at 9:10 AM. PesaNova balance is Ksh 12,210.00.",
                context =
                    FinancialInterpretationContext(
                        learnedAccounts =
                            listOf(learnedPesaNova)
                    )
            )

        val payment =
            interpretation.events.single() as
                PaymentEventFact

        assertEquals(
            "discovered:pesanova",
            payment.source.entityKey
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            payment.source.ownershipHint
        )
        assertEquals(
            1_221_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
        assertTrue(
            interpretation
                .consumesLegacyTransactionParsing
        )
    }

    @Test
    fun learnedExternalAccountIsNotPromotedToUserOwnedSource() {
        val external =
            learnedPesaNova.copy(
                ownershipHint =
                    AccountOwnershipHint.EXTERNAL
            )

        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "PesaNova",
                body =
                    "TST12LRN02 Confirmed. Ksh 640.00 sent to SAMPLE PERSON 0712000000 on 28/9/26 at 9:10 AM. PesaNova balance is Ksh 12,210.00.",
                context =
                    FinancialInterpretationContext(
                        learnedAccounts =
                            listOf(external)
                    )
            )

        assertTrue(
            interpretation.events.isEmpty()
        )
        assertEquals(
            AccountOwnershipHint.EXTERNAL,
            interpretation.balances
                .single()
                .account
                .ownershipHint
        )
    }

    @Test
    fun learnedAccountCanParticipateInOwnAccountTransfer() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body =
                    "TST12LRN03 Confirmed. Ksh 5,000.00 transferred from PesaNova account on 28/9/26 at 9:20 AM. PesaNova balance is Ksh 7,210.00. M-PESA balance is Ksh 5,000.00.",
                context =
                    FinancialInterpretationContext(
                        learnedAccounts =
                            listOf(learnedPesaNova)
                    )
            )

        val transfer =
            interpretation.events.single() as
                TransferEventFact

        assertEquals(
            "discovered:pesanova",
            transfer.source.entityKey
        )
        assertEquals(
            "SAFARICOM_MPESA",
            transfer.destination.providerKey
        )
        assertEquals(
            InterpretationConfidence.HIGH,
            transfer.confidence
        )
    }

    @Test
    fun learnedContextCannotOverrideBuiltInProviderKnowledge() {
        val conflicting =
            learnedPesaNova.copy(
                entityKey = "discovered:mpesa",
                displayName = "M-Pesa",
                ownershipHint =
                    AccountOwnershipHint.EXTERNAL
            )

        val resolved =
            FinancialProviderRegistry.resolveAccount(
                rawLabel = "M-Pesa",
                context =
                    FinancialInterpretationContext(
                        learnedAccounts =
                            listOf(conflicting)
                    )
            )

        assertEquals(
            "SAFARICOM_MPESA",
            resolved.providerKey
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            resolved.ownershipHint
        )
    }
}

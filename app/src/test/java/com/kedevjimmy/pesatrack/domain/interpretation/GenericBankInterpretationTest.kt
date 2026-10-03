package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenericBankInterpretationTest {
    @Test
    fun understandsBankCreditAndSeparatesAvailableBalance() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Your account has been credited with KES 60,000.00 from XYZ COMPANY on 26/09/2026. Available balance KES 72,100.00."
            )

        val receipt =
            interpretation.events.single() as
                ReceiptEventFact

        assertEquals(6_000_000L, receipt.amountMinor)
        assertEquals("XYZ COMPANY", receipt.counterparty)
        assertEquals(
            "My Bank",
            receipt.destination.displayName
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            receipt.destination.ownershipHint
        )
        assertEquals(
            FinancialAccountNature.ASSET,
            receipt.destination.nature
        )
        assertEquals(
            InterpretationConfidence.HIGH,
            receipt.confidence
        )

        val balance =
            interpretation.balances.single()
        assertEquals(
            7_210_000L,
            balance.balanceMinor
        )
        assertEquals(
            receipt.destination.entityKey,
            balance.account.entityKey
        )
        assertTrue(
            interpretation
                .consumesLegacyTransactionParsing
        )
    }

    @Test
    fun understandsExplicitBankDebitWithoutInventingCounterparty() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "EQUITY BANK",
                body =
                    "Your Equity a/c XXX1234 has been debited with KES 1,250.00 on 03/10/26. Available balance KES 8,750.00. Ref FT26276ABC."
            )

        assertTrue(
            interpretation.recognized
        )
        assertTrue(
            interpretation
                .consumesLegacyTransactionParsing
        )

        val debit =
            interpretation.events.single() as
                UnresolvedFinancialEventFact

        assertEquals(
            125_000L,
            debit.amountMinor
        )
        assertEquals(
            "KENYAN_BANK_EQUITY",
            debit.observedAccount
                ?.providerKey
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            debit.observedAccount
                ?.ownershipHint
        )
        assertEquals(
            InterpretationConfidence.MEDIUM,
            debit.confidence
        )
        assertEquals(
            FinancialMovementDirectionHint
                .OUTGOING,
            debit.directionHint
        )
        assertTrue(
            debit.evidence.any {
                it.field ==
                    FinancialField
                        .ACCOUNT_REFERENCE
            }
        )
        assertEquals(
            875_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun brandedAccountDebitWithBeneficiaryStillUsesPaymentPath() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "EQUITY BANK",
                body =
                    "Your Equity a/c XXX5678 has been debited with KES 2,400.00. Transfer to SAMPLE BENEFICIARY completed. Available balance KES 6,350.00."
            )

        val payment =
            interpretation.events.single() as
                PaymentEventFact

        assertEquals(
            240_000L,
            payment.amountMinor
        )
        assertEquals(
            "SAMPLE BENEFICIARY",
            payment.counterparty
        )
        assertEquals(
            "KENYAN_BANK_EQUITY",
            payment.source.providerKey
        )
    }

    @Test
    fun genericBankDebitWithoutCounterpartyUsesOwnedMessageSource() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Your account XX9087 has been debited by Ksh 825.50 on 03/10/26. Available balance KES 4,100.00."
            )

        val debit =
            interpretation.events.single() as
                UnresolvedFinancialEventFact

        assertEquals(
            82_550L,
            debit.amountMinor
        )
        assertEquals(
            "My Bank",
            debit.observedAccount
                ?.displayName
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            debit.observedAccount
                ?.ownershipHint
        )
    }

    @Test
    fun genericUnresolvedRelationshipDoesNotInventDirection() {
        val interpretation =
            FinancialEventClassifier.classify(
                ResolvedFinancialMessage(
                    transactionCode = null,
                    occurredAt = null,
                    balances = emptyList(),
                    relationship =
                        ResolvedUnresolvedMovementRelationship(
                            amountMinor = 92_500L,
                            observedAccount = null,
                            feeMinor = 0L,
                            confidence =
                                InterpretationConfidence.LOW,
                            evidence = emptyList()
                        )
                )
            )

        val unresolved =
            interpretation.events.single() as
                UnresolvedFinancialEventFact

        assertEquals(
            null,
            unresolved.directionHint
        )
    }

    @Test
    fun understandsExplicitBankCreditWithoutInventingCounterparty() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "EQUITY BANK",
                body =
                    "Your Equity a/c XXX1234 has been credited with KES 5,000.00 on 03/10/26. Available balance KES 13,750.00. Ref FT26276XYZ."
            )

        assertTrue(
            interpretation.recognized
        )
        assertTrue(
            interpretation
                .consumesLegacyTransactionParsing
        )

        val credit =
            interpretation.events.single() as
                UnresolvedFinancialEventFact

        assertEquals(
            500_000L,
            credit.amountMinor
        )
        assertEquals(
            FinancialMovementDirectionHint
                .INCOMING,
            credit.directionHint
        )
        assertEquals(
            "KENYAN_BANK_EQUITY",
            credit.observedAccount
                ?.providerKey
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            credit.observedAccount
                ?.ownershipHint
        )
        assertEquals(
            InterpretationConfidence.MEDIUM,
            credit.confidence
        )
        assertTrue(
            credit.evidence.any {
                it.field ==
                    FinancialField
                        .ACCOUNT_REFERENCE
            }
        )
        assertEquals(
            1_375_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun amountFirstBankCreditWithoutCounterpartyIsIncoming() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "KES 825.50 has been credited to your account XX9087 on 03/10/26. Available balance KES 4,925.50."
            )

        val credit =
            interpretation.events.single() as
                UnresolvedFinancialEventFact

        assertEquals(
            82_550L,
            credit.amountMinor
        )
        assertEquals(
            FinancialMovementDirectionHint
                .INCOMING,
            credit.directionHint
        )
        assertEquals(
            "My Bank",
            credit.observedAccount
                ?.displayName
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            credit.observedAccount
                ?.ownershipHint
        )
    }

    @Test
    fun creditedFromCounterpartyStillUsesReceiptPath() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Your account has been credited with KES 4,500.00 from SAMPLE EMPLOYER on 03/10/26. Available balance KES 9,600.00."
            )

        val receipt =
            interpretation.events.single() as
                ReceiptEventFact

        assertEquals(
            450_000L,
            receipt.amountMinor
        )
        assertEquals(
            "SAMPLE EMPLOYER",
            receipt.counterparty
        )
    }

    @Test
    fun understandsAmountFirstReceivedFromBankWording() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "KES 60,000.00 received from My Employer. Available balance KES 72,100.00."
            )

        val receipt =
            interpretation.events.single() as
                ReceiptEventFact

        assertEquals(6_000_000L, receipt.amountMinor)
        assertEquals("My Employer", receipt.counterparty)
        assertEquals(
            7_210_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun understandsSuccessfulCardPurchaseAtMerchant() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Purchase of KES 1,250.00 at SAMPLE CAFE using your card ending 1234 was successful. Available balance KES 7,500.00."
            )

        val payment =
            interpretation.events.single() as
                PaymentEventFact

        assertEquals(
            125_000L,
            payment.amountMinor
        )
        assertEquals(
            "SAMPLE CAFE",
            payment.counterparty
        )
        assertEquals(
            PaymentRouteHint.MERCHANT,
            payment.routeHint
        )
        assertEquals(
            "My Bank",
            payment.source.displayName
        )
        assertEquals(
            750_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun cardUsedWordingPreservesTransactionFee() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Your card ending 9876 was used for Ksh 2,400.00 at SAMPLE STORE. Transaction fee KES 30.00. Available balance KES 4,100.00."
            )

        val payment =
            interpretation.events.single() as
                PaymentEventFact

        assertEquals(
            240_000L,
            payment.amountMinor
        )
        assertEquals(
            3_000L,
            payment.feeMinor
        )
        assertEquals(
            "SAMPLE STORE",
            payment.counterparty
        )
    }

    @Test
    fun declinedCardPurchaseDoesNotCreateFinancialEvent() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Purchase of KES 3,000.00 at SAMPLE STORE using your card ending 1234 was declined."
            )

        assertTrue(
            interpretation.events.isEmpty()
        )
    }

    @Test
    fun understandsAtmWithdrawalFromOwnedBankAccount() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "EQUITY BANK",
                body =
                    "Cash withdrawal of KES 5,000.00 from your account XXX1234 at SAMPLE ATM on 03/10/26. Available balance KES 10,000.00."
            )

        val withdrawal =
            interpretation.events.single() as
                CashWithdrawalEventFact

        assertEquals(
            500_000L,
            withdrawal.amountMinor
        )
        assertEquals(
            "KENYAN_BANK_EQUITY",
            withdrawal.source.providerKey
        )
        assertEquals(
            "SAMPLE ATM",
            withdrawal.counterparty
        )
        assertEquals(
            1_000_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun failedAtmWithdrawalDoesNotCreateFinancialEvent() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "EQUITY BANK",
                body =
                    "Cash withdrawal of KES 5,000.00 from your account XXX1234 at SAMPLE ATM failed."
            )

        assertTrue(
            interpretation.events.isEmpty()
        )
    }


    @Test
    fun understandsCashDepositIntoOwnedBankAccount() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "EQUITY BANK",
                body =
                    "Cash deposit of KES 3,500.00 into your account XXX1234 at SAMPLE BRANCH on 03/10/26. Available balance KES 13,500.00."
            )

        val deposit =
            interpretation.events.single() as
                CashDepositEventFact

        assertEquals(
            350_000L,
            deposit.amountMinor
        )
        assertEquals(
            "KENYAN_BANK_EQUITY",
            deposit.destination.providerKey
        )
        assertEquals(
            "SAMPLE BRANCH",
            deposit.counterparty
        )
        assertEquals(
            1_350_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun failedCashDepositDoesNotCreateFinancialEvent() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "EQUITY BANK",
                body =
                    "Cash deposit of KES 3,500.00 into your account XXX1234 at SAMPLE BRANCH was unsuccessful."
            )

        assertTrue(
            interpretation.events.isEmpty()
        )
    }


    @Test
    fun understandsAvailBalAbbreviationOnBankDebit() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "EQUITY BANK",
                body =
                    "Your Equity a/c XXX1234 has been debited with KES 1,250.00 on 03/10/26. Avail Bal: KES 8,750.00."
            )

        val debit =
            interpretation.events.single() as
                UnresolvedFinancialEventFact

        assertEquals(
            125_000L,
            debit.amountMinor
        )
        assertEquals(
            875_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
        assertEquals(
            "KENYAN_BANK_EQUITY",
            interpretation.balances
                .single()
                .account
                .providerKey
        )
    }

    @Test
    fun understandsAvlBalAbbreviationOnBankCredit() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "KES 825.50 has been credited to your account XX9087 on 03/10/26. AVL BAL Ksh 4,925.50."
            )

        val credit =
            interpretation.events.single() as
                UnresolvedFinancialEventFact

        assertEquals(
            FinancialMovementDirectionHint
                .INCOMING,
            credit.directionHint
        )
        assertEquals(
            492_550L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun understandsCurrentBalAbbreviation() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Your account XX9087 has been debited by KES 500.00. Current Bal is KES 3,600.00."
            )

        assertEquals(
            360_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun understandsAccountAnchoredDrAsOutgoingMovement() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Your acct XX9087 DR KES 750.00 on 03/10/26. New Bal KES 4,250.00."
            )

        val debit =
            interpretation.events.single() as
                UnresolvedFinancialEventFact

        assertEquals(
            75_000L,
            debit.amountMinor
        )
        assertEquals(
            FinancialMovementDirectionHint
                .OUTGOING,
            debit.directionHint
        )
        assertEquals(
            425_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
        assertTrue(
            debit.evidence.any {
                it.field ==
                    FinancialField
                        .ACCOUNT_REFERENCE
            }
        )
    }

    @Test
    fun understandsAccountAnchoredCrAsIncomingMovement() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "A/C XX9087 CR: Ksh 1,200.00 on 03/10/26. Bal KES 5,450.00."
            )

        val credit =
            interpretation.events.single() as
                UnresolvedFinancialEventFact

        assertEquals(
            120_000L,
            credit.amountMinor
        )
        assertEquals(
            FinancialMovementDirectionHint
                .INCOMING,
            credit.directionHint
        )
        assertEquals(
            545_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun bareDrWithoutAccountDoesNotInventMovement() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "DR KES 750.00 on 03/10/26."
            )

        assertTrue(
            interpretation.events.isEmpty()
        )
    }

    @Test
    fun pendingAccountDrDoesNotCreatePostedMovement() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Your acct XX9087 DR KES 750.00 pending."
            )

        assertTrue(
            interpretation.events.isEmpty()
        )
    }

    @Test
    fun understandsPlainBalShorthandOnBankDebit() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Your account XX9087 has been debited by KES 500.00. Bal: KES 3,600.00."
            )

        assertEquals(
            360_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun understandsPlainBalShorthandOnBankCredit() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "KES 825.50 has been credited to your account XX9087. BAL Ksh 4,925.50."
            )

        assertEquals(
            492_550L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun understandsNewBalAfterBankDebit() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Your account XX9087 has been debited by KES 500.00. New Bal KES 3,100.00."
            )

        assertEquals(
            310_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun understandsNewBalanceAfterBankCredit() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "KES 825.50 has been credited to your account XX9087. New Balance: Ksh 4,925.50."
            )

        assertEquals(
            492_550L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun bookBalIsNotSilentlyTreatedAsAvailableBalance() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Your account XX9087 has been debited by KES 500.00. Book Bal KES 3,600.00."
            )

        assertTrue(
            interpretation.balances.isEmpty()
        )
    }

    @Test
    fun ledgerBalIsNotSilentlyTreatedAsAvailableBalance() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Your account XX9087 has been debited by KES 500.00. Ledger Bal KES 3,600.00."
            )

        assertTrue(
            interpretation.balances.isEmpty()
        )
    }

    @Test
    fun standaloneBankChargeDoesNotPopulateEmbeddedFeeCandidate() {
        val candidates =
            FinancialCandidateExtractor
                .extract(
                    FinancialMessageNormalizer
                        .normalize(
                            title = "My Bank",
                            body =
                                "Monthly service charge KES 50.00 has been debited from your account XX9087."
                        )
                )

        assertEquals(
            null,
            candidates.fee
        )
        assertEquals(
            5_000L,
            candidates
                .standaloneFee
                ?.amountMinor
        )
    }

    @Test
    fun debtAccessFeeStillUsesEmbeddedFeeCandidate() {
        val candidates =
            FinancialCandidateExtractor
                .extract(
                    FinancialMessageNormalizer
                        .normalize(
                            title = "Finance",
                            body =
                                "Nova Credit amount is Ksh 500.00. Access Fee charged Ksh 10.00. Total Nova Credit outstanding amount is Ksh 510.00."
                        )
                )

        assertEquals(
            1_000L,
            candidates.fee
                ?.amountMinor
        )
        assertEquals(
            null,
            candidates.standaloneFee
        )
    }

    @Test
    fun understandsPostedStandaloneBankServiceCharge() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Monthly service charge KES 50.00 has been debited from your account XX9087. New Bal KES 4,950.00."
            )

        val fee =
            interpretation.events.single() as
                FeeEventFact

        assertEquals(
            5_000L,
            fee.amountMinor
        )
        assertEquals(
            "My Bank",
            fee.chargedTo.displayName
        )
        assertEquals(
            AccountOwnershipHint.USER_OWNED,
            fee.chargedTo.ownershipHint
        )
        assertEquals(
            495_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )

        val canonical =
            interpretation
                .toCanonicalDraft()
                .records
                .filterIsInstance<
                    FeeDraft
                >()
                .single()
        assertEquals(
            5_000L,
            canonical.amountMinor
        )
        assertEquals(
            fee.chargedTo.entityKey,
            canonical.chargedTo
                ?.entityKey
        )
    }

    @Test
    fun understandsAccountFirstMaintenanceFeePosting() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Your account XX9087 was charged KES 100.00 as monthly maintenance fee. Avail Bal KES 4,900.00."
            )

        val fee =
            interpretation.events.single() as
                FeeEventFact

        assertEquals(
            10_000L,
            fee.amountMinor
        )
        assertEquals(
            490_000L,
            interpretation.balances
                .single()
                .balanceMinor
        )
    }

    @Test
    fun futureBankFeeNoticeDoesNotCreatePostedMovement() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Your account XX9087 will be charged KES 50.00 as monthly maintenance fee from next month."
            )

        assertTrue(
            interpretation.events.isEmpty()
        )
    }

    @Test
    fun informationalFeeAmountDoesNotCreateStandaloneCharge() {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "My Bank",
                body =
                    "Monthly maintenance fee KES 50.00 applies to this account."
            )

        assertTrue(
            interpretation.events.isEmpty()
        )
    }

}

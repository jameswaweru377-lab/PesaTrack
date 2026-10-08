package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.interpretation.*
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferMatch
import com.kedevjimmy.pesatrack.domain.rules.OwnTransferEvidence
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutomaticAccountResolutionTest {
    private lateinit var context: Context

    @Before fun freshInstall() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
        assertNull(AccountStore.get(context).listAccounts().first {
            it.id == AccountStore.DEFAULT_MPESA_ID
        }.identifier)
    }

    @Test fun mshwariTransferAndBalancesWorkWithoutPhoneSetupAndReplayOnlyOnce() {
        val coordinator = FinancialMessageIngestionCoordinator.get(context)
        val first = coordinator.ingest("MPESA", TRANSFER, "test.sms", "TEST_SMS", EPOCH, false)
        assertEquals(0, first.pendingReviewEvents)
        assertEquals(1, first.insertedTransactions)
        val store = FinancialUnderstandingStore.get(context)
        assertTrue(store.isUserOwned("provider:SAFARICOM_MPESA"))
        assertTrue(store.isUserOwned("provider:NCBA_MSHWARI_SAVINGS"))
        assertEquals(2, store.latestBalances().size)
        assertEquals(MoneyDirection.TRANSFER.name,
            TransactionStore.get(context).findByTransactionCode("RQK12ABC34")?.direction)
        assertEquals(0, coordinator.ingest("MPESA", TRANSFER, "test.sms", "TEST_SMS", EPOCH, false).insertedTransactions)
    }

    @Test fun recognizedBankWaitsForOwnershipThenReplayAttachesCorrectAccount() {
        val coordinator =
            FinancialMessageIngestionCoordinator.get(context)
        val body =
            "Your transaction of Kenya Shillings 20,000 has been credited to SAMPLE PERSON. Reference number EQA12ABC34. M-Pesa reference UFT12ABC34."

        val beforeConfirmation =
            coordinator.ingest(
                "EQUITY",
                body,
                "test.sms",
                "TEST_SMS",
                EPOCH,
                false
            )

        assertEquals(
            0,
            beforeConfirmation.insertedTransactions
        )
        assertFalse(
            AccountStore.get(context)
                .listAccounts()
                .any {
                    it.name ==
                        "Equity Bank Kenya"
                }
        )

        val equity =
            FinancialProviderRegistry
                .resolveOwnedMessageSourceAccount(
                    "EQUITY",
                    true
                )!!
        val accountId =
            FinancialUnderstandingStore
                .get(context)
                .setInstitutionOwned(
                    reference = equity,
                    identifier =
                        "1234567890"
                )
        assertNotNull(accountId)

        val afterConfirmation =
            coordinator.ingest(
                "EQUITY",
                body,
                "test.sms",
                "TEST_SMS",
                EPOCH,
                false
            )

        assertEquals(
            1,
            afterConfirmation.insertedTransactions
        )
        assertEquals(
            accountId,
            TransactionStore.get(context)
                .recent()
                .single()
                .accountId
        )
    }

    @Test fun explicitBankSelectionAttachesBalanceAndExternalDecisionPersists() {
        val store =
            FinancialUnderstandingStore.get(context)
        val known =
            FinancialProviderRegistry
                .resolveOwnedMessageSourceAccount(
                    "EQUITY",
                    true
                )!!
        val unknown =
            known.copy(
                ownershipHint =
                    AccountOwnershipHint.UNKNOWN
            )
        val fact =
            BalanceObservationFact(
                unknown,
                50_000L,
                InterpretationConfidence.HIGH
            )

        store.recordBalance(
            fact,
            "same-message",
            EPOCH,
            "test.sms",
            "TEST_SMS"
        )
        assertNull(
            store.latestBalances()
                .single()
                .accountId
        )

        val accountId =
            store.setInstitutionOwned(
                reference = known,
                identifier = "1234567890"
            )
        assertNotNull(accountId)

        // Confirmation immediately attaches any previously observed
        // balance for the same institution; replay is no longer required.
        assertEquals(
            accountId,
            store.latestBalances()
                .single()
                .accountId
        )
        assertEquals(
            "1234567890",
            AccountStore.get(context)
                .listAccounts()
                .first {
                    it.id == accountId
                }
                .identifier
        )

        store.markEntityExternal(
            known.entityKey
        )
        assertNull(
            store.observeEntity(
                known,
                EPOCH + 1
            )
        )
        assertFalse(
            store.isUserOwned(
                known.entityKey
            )
        )
        assertFalse(
            AccountStore.get(context)
                .listAccounts(
                    enabledOnly = false
                )
                .first {
                    it.id == accountId
                }
                .enabled
        )
    }

    @Test
    fun historicalRowCanBeReclassifiedAfterOwnershipSelection() {
        val accountStore =
            AccountStore.get(context)
        val bank =
            accountStore.save(
                id = "owned-bank-test",
                name = "KCB Bank Kenya",
                type = AccountType.BANK
            )
        val transactionStore =
            TransactionStore.get(context)
        val parsed =
            ParsedMpesaTransaction(
                transactionCode =
                    "OWN12ABC34",
                type =
                    MpesaTransactionType
                        .SENT_MONEY,
                amountMinor =
                    2_500_00L,
                counterparty =
                    "KCB Bank Kenya",
                counterpartyPhone =
                    null,
                feeMinor = 0L,
                balanceMinor = null,
                occurredAtText = null,
                rawMessage =
                    "Ksh2,500 transferred to KCB account"
            )
        val ownTransfer =
            OwnAccountTransferMatch(
                sourceAccountId =
                    AccountStore
                        .DEFAULT_MPESA_ID,
                sourceName = "M-Pesa",
                destinationAccountId =
                    bank.id,
                destinationName =
                    bank.name,
                evidence =
                    OwnTransferEvidence
                        .EXACT_IDENTIFIER
            )

        assertTrue(
            transactionStore.insertIfNew(
                parsed = parsed,
                sourcePackage = null,
                sourceKind =
                    "test_history",
                receivedAtEpochMs = EPOCH,
                occurredAtEpochMs = EPOCH,
                rebuildPayCycles = false,
                runRelationshipReconciliation =
                    false,
                runMirrorCorrelation = false,
                accountTransferOverride =
                    ownTransfer
            )
        )

        assertEquals(
            1,
            transactionStore
                .reconcileAutoTransfersWithOwnedAccounts(
                    setOf(
                        AccountStore
                            .DEFAULT_MPESA_ID
                    )
                )
        )
        val demoted =
            transactionStore
                .findByTransactionCode(
                    "OWN12ABC34"
                )!!
        assertEquals(
            MoneyDirection.EXPENSE.name,
            demoted.direction
        )
        assertNull(
            demoted.linkedAccountId
        )

        assertFalse(
            transactionStore.insertIfNew(
                parsed = parsed,
                sourcePackage = null,
                sourceKind =
                    "test_history",
                receivedAtEpochMs = EPOCH,
                occurredAtEpochMs = EPOCH,
                rebuildPayCycles = false,
                runRelationshipReconciliation =
                    false,
                runMirrorCorrelation = false,
                accountTransferOverride =
                    ownTransfer
            )
        )

        val promoted =
            transactionStore
                .findByTransactionCode(
                    "OWN12ABC34"
                )!!
        assertEquals(
            MoneyDirection.TRANSFER.name,
            promoted.direction
        )
        assertEquals(
            TransactionCategory
                .TRANSFER.name,
            promoted.category
        )
        assertEquals(
            AccountStore
                .DEFAULT_MPESA_ID,
            promoted.accountId
        )
        assertEquals(
            bank.id,
            promoted.linkedAccountId
        )
    }

    companion object {
        private const val EPOCH = 1_790_613_600_000L
        private const val TRANSFER = "RQK12ABC34 Confirmed. Ksh 7,000.00 transferred from M-Shwari account on 28/9/26 at 6:00 PM. M-Shwari balance is Ksh 543.00. M-PESA balance is Ksh 7,789.00."
    }
}

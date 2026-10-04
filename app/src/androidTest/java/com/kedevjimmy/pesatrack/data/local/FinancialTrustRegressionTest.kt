package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.interpretation.CanonicalFinancialRecordKind
import com.kedevjimmy.pesatrack.domain.interpretation.PaymentRouteHint
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.RulePreset
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinancialTrustRegressionTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context =
            ApplicationProvider
                .getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun repeatedUnknownMerchantStaysAutomaticInsteadOfBecomingReviewWork() {
        val store =
            TransactionStore.get(context)

        listOf(
            "TRUST00001" to
                TEST_EPOCH_MS,
            "TRUST00002" to
                (TEST_EPOCH_MS + 60_000L)
        ).forEach { (code, epochMs) ->
            assertTrue(
                store
                    .insertInterpretedMoneyMovementIfNew(
                        kind =
                            CanonicalFinancialRecordKind
                                .PAYMENT,
                        paymentRouteHint =
                            PaymentRouteHint.MERCHANT,
                        transactionCode = code,
                        amountMinor = 12_500L,
                        counterparty =
                            "Example recurring merchant",
                        counterpartyPhone = null,
                        feeMinor = 0L,
                        balanceMinor = null,
                        occurredAtText = null,
                        rawMessage =
                            "Regression fixture",
                        sourcePackage =
                            "test.package",
                        sourceKind =
                            "interpreted_TEST",
                        receivedAtEpochMs =
                            epochMs,
                        occurredAtEpochMs =
                            epochMs,
                        matchedRule = null,
                        rebuildPayCycles = false,
                        accountIdOverride =
                            AccountStore
                                .DEFAULT_MPESA_ID
                    )
            )
        }

        store.reconcileReviewTriageForMonth(
            epochMs = TEST_EPOCH_MS
        )

        listOf(
            "TRUST00001",
            "TRUST00002"
        ).forEach { code ->
            val stored =
                store.findByTransactionCode(code)
            assertEquals(
                ReviewState
                    .AUTO_CLASSIFIED.name,
                stored?.reviewState
            )
            assertEquals(
                TransactionCategory
                    .UNCATEGORIZED.name,
                stored?.category
            )
        }
    }

    @Test
    fun genericRecurringPatternDoesNotReserveCycleMoney() {
        val ruleStore =
            RuleStore.get(context)
        val transactionStore =
            TransactionStore.get(context)
        val payCycleStore =
            PayCycleStore.get(context)

        val salaryRule =
            ruleStore.save(
                name = "Salary",
                counterpartyContains = "",
                counterpartyExact =
                    "Employer",
                category =
                    TransactionCategory.INCOME,
                direction =
                    MoneyDirection.INCOME,
                displayLabel = "Salary",
                preset =
                    RulePreset.SALARY,
                startsPayCycle = true,
                accountId =
                    AccountStore.DEFAULT_MPESA_ID
            )

        assertTrue(
            transactionStore.insertManualIncome(
                amountMinor = 6_000_000L,
                occurredAtEpochMs =
                    TEST_EPOCH_MS,
                label = "Salary",
                matchedRule =
                    salaryRule
            )
        )

        ruleStore.save(
            name = "Rent",
            counterpartyContains = "",
            counterpartyExact =
                "Landlord",
            category =
                TransactionCategory.RENT,
            direction =
                MoneyDirection.EXPENSE,
            preset =
                RulePreset.RENT,
            plannedAmountMinor =
                1_000_000L
        )
        ruleStore.save(
            name =
                "Frequent transport recurring",
            counterpartyContains = "",
            counterpartyExact =
                "Regular Matatu",
            category =
                TransactionCategory.TRANSPORT,
            direction =
                MoneyDirection.EXPENSE,
            preset =
                RulePreset.OTHER_RECURRING,
            plannedAmountMinor =
                1_500_000L
        )

        payCycleStore.rebuild()
        val snapshot =
            payCycleStore.snapshot(
                nowEpochMs =
                    TEST_EPOCH_MS +
                        86_400_000L
            )

        assertEquals(
            1_000_000L,
            snapshot
                .remainingCommitmentsMinor
        )
        assertEquals(
            5_000_000L,
            snapshot
                .availableToLiveMinor
        )
    }

    private companion object {
        const val DATABASE_NAME =
            "pesatrack.db"
        const val TEST_EPOCH_MS =
            1_780_588_800_000L
    }
}

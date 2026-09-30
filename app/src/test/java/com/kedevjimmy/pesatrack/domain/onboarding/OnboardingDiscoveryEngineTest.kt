package com.kedevjimmy.pesatrack.domain.onboarding

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnboardingDiscoveryEngineTest {
    private val utc =
        TimeZone.getTimeZone("UTC")

    @Test
    fun repeatedIncomingPayerAcrossMonthsCanBeSuggestedAsSalary() {
        val observations = listOf(
            observation(
                label = "Sample Employer",
                amountMinor = 60_000_00L,
                month = Calendar.AUGUST,
                day = 25,
                direction = MoneyDirection.INCOME,
                accountId = "bank-equity"
            ),
            observation(
                label = "Sample Employer",
                amountMinor = 60_000_00L,
                month = Calendar.SEPTEMBER,
                day = 26,
                direction = MoneyDirection.INCOME,
                accountId = "bank-equity"
            ),
            observation(
                label = "One-off Refund",
                amountMinor = 80_000_00L,
                month = Calendar.SEPTEMBER,
                day = 4,
                direction = MoneyDirection.INCOME,
                accountId = "bank-equity"
            )
        )

        val suggestion =
            OnboardingDiscoveryEngine
                .salarySuggestion(
                    observations,
                    utc
                )

        assertEquals(
            "Sample Employer",
            suggestion?.label
        )
        assertEquals(
            "bank-equity",
            suggestion?.accountId
        )
        assertEquals(
            2,
            suggestion?.monthCount
        )
    }

    @Test
    fun oneIncomingPaymentIsNotEnoughToCallSalary() {
        val suggestion =
            OnboardingDiscoveryEngine
                .salarySuggestion(
                    listOf(
                        observation(
                            label = "Sample Employer",
                            amountMinor = 60_000_00L,
                            month = Calendar.SEPTEMBER,
                            day = 26,
                            direction =
                                MoneyDirection.INCOME,
                            accountId = "bank-equity"
                        )
                    ),
                    utc
                )

        assertNull(suggestion)
    }

    @Test
    fun repeatedPhoneFromNonMpesaAccountCanBeAskedForOwnership() {
        val phone =
            OnboardingDiscoveryEngine
                .mpesaPhoneSuggestion(
                    observations = listOf(
                        observation(
                            label = "Sample Person",
                            amountMinor = 20_000_00L,
                            month = Calendar.AUGUST,
                            day = 10,
                            direction =
                                MoneyDirection.EXPENSE,
                            accountId = "bank-equity",
                            phone = "254700000001"
                        ),
                        observation(
                            label = "Sample Person",
                            amountMinor = 5_000_00L,
                            month = Calendar.SEPTEMBER,
                            day = 11,
                            direction =
                                MoneyDirection.EXPENSE,
                            accountId = "bank-equity",
                            phone = "0700000001"
                        )
                    ),
                    mpesaAccountId = "account-mpesa"
                )

        assertEquals(
            "+254700000001",
            phone
        )
    }

    @Test
    fun mpesaCounterpartyPhonesAreNotTreatedAsOwnersNumber() {
        val phone =
            OnboardingDiscoveryEngine
                .mpesaPhoneSuggestion(
                    observations = listOf(
                        observation(
                            label = "Friend",
                            amountMinor = 1_000_00L,
                            month = Calendar.AUGUST,
                            day = 10,
                            direction =
                                MoneyDirection.EXPENSE,
                            accountId = "account-mpesa",
                            phone = "0712000000"
                        ),
                        observation(
                            label = "Friend",
                            amountMinor = 1_500_00L,
                            month = Calendar.SEPTEMBER,
                            day = 11,
                            direction =
                                MoneyDirection.EXPENSE,
                            accountId = "account-mpesa",
                            phone = "0712000000"
                        )
                    ),
                    mpesaAccountId = "account-mpesa"
                )

        assertNull(phone)
    }

    @Test
    fun tinyRecurringCreditsAreNotAutoSuggestedAsSalary() {
        val suggestion =
            OnboardingDiscoveryEngine
                .salarySuggestion(
                    listOf(
                        observation(
                            label = "QUIDEXPLUS KENYA LIMITED",
                            amountMinor = 220_00L,
                            month = Calendar.AUGUST,
                            day = 15,
                            direction =
                                MoneyDirection.INCOME,
                            accountId = "bank-equity"
                        ),
                        observation(
                            label = "QUIDEXPLUS KENYA LIMITED",
                            amountMinor = 220_00L,
                            month = Calendar.SEPTEMBER,
                            day = 15,
                            direction =
                                MoneyDirection.INCOME,
                            accountId = "bank-equity"
                        )
                    ),
                    utc
                )

        assertNull(suggestion)
    }

    @Test
    fun merchantSettlementLanguageIsNotAutoSuggestedAsSalary() {
        val suggestion =
            OnboardingDiscoveryEngine
                .salarySuggestion(
                    listOf(
                        observation(
                            label = "SAMPLE PAYMENTS SETTLEMENT",
                            amountMinor = 15_000_00L,
                            month = Calendar.AUGUST,
                            day = 15,
                            direction =
                                MoneyDirection.INCOME,
                            accountId = "bank-equity"
                        ),
                        observation(
                            label = "SAMPLE PAYMENTS SETTLEMENT",
                            amountMinor = 15_000_00L,
                            month = Calendar.SEPTEMBER,
                            day = 15,
                            direction =
                                MoneyDirection.INCOME,
                            accountId = "bank-equity"
                        )
                    ),
                    utc
                )

        assertNull(suggestion)
    }


    private fun observation(
        label: String,
        amountMinor: Long,
        month: Int,
        day: Int,
        direction: MoneyDirection,
        accountId: String,
        phone: String? = null
    ): OnboardingTransactionObservation =
        OnboardingTransactionObservation(
            counterparty = label,
            counterpartyPhone = phone,
            amountMinor = amountMinor,
            occurredAtEpochMs =
                Calendar.getInstance(utc)
                    .apply {
                        clear()
                        set(
                            2026,
                            month,
                            day,
                            12,
                            0,
                            0
                        )
                    }
                    .timeInMillis,
            direction = direction,
            accountId = accountId
        )
}

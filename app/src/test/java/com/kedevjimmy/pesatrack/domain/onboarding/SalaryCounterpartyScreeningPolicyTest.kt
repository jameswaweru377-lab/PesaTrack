package com.kedevjimmy.pesatrack.domain.onboarding

import com.kedevjimmy.pesatrack.domain.intelligence.EntityMatchConfidence
import com.kedevjimmy.pesatrack.domain.intelligence.RemoteEntityEnrichment
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SalaryCounterpartyScreeningPolicyTest {
    @Test
    fun paymentProcessorDomainIsNotSuggestedAsSalary() {
        val enrichment =
            RemoteEntityEnrichment(
                canonicalName =
                    "Quidexplus Kenya Limited",
                domain = "pawapay.io",
                logoUrl = null,
                categoryHint = null,
                confidence =
                    EntityMatchConfidence.LIKELY,
                source = "remote"
            )

        assertTrue(
            SalaryCounterpartyScreeningPolicy
                .shouldSuppress(
                    label =
                        "QUIDEXPLUS KENYA LIMITED",
                    enrichment =
                        enrichment
                )
        )
    }

    @Test
    fun ordinaryEmployerNameIsNotSuppressedWithoutMerchantEvidence() {
        assertEquals(
            false,
            SalaryCounterpartyScreeningPolicy
                .shouldSuppress(
                    label =
                        "SAMPLE SCHOOL LIMITED",
                    enrichment = null
                )
        )
    }

    @Test
    fun merchantCategoryIsNotSuggestedAsSalary() {
        val enrichment =
            RemoteEntityEnrichment(
                canonicalName =
                    "Sample Transport",
                domain =
                    "sampletransport.co.ke",
                logoUrl = null,
                categoryHint =
                    TransactionCategory.TRANSPORT,
                confidence =
                    EntityMatchConfidence.HIGH,
                source = "remote"
            )

        assertTrue(
            SalaryCounterpartyScreeningPolicy
                .shouldSuppress(
                    label = "Sample Transport",
                    enrichment =
                        enrichment
                )
        )
    }

    @Test
    fun excludedRecurringIncomeLetsNextSalaryCandidateWin() {
        val zone =
            TimeZone.getTimeZone(
                "Africa/Nairobi"
            )
        val observations =
            listOf(
                income(
                    "PAYMENT GATEWAY LTD",
                    220_00L,
                    2026,
                    Calendar.AUGUST,
                    3
                ),
                income(
                    "PAYMENT GATEWAY LTD",
                    220_00L,
                    2026,
                    Calendar.SEPTEMBER,
                    3
                ),
                income(
                    "REAL EMPLOYER LIMITED",
                    52_000_00L,
                    2026,
                    Calendar.AUGUST,
                    28
                ),
                income(
                    "REAL EMPLOYER LIMITED",
                    52_000_00L,
                    2026,
                    Calendar.SEPTEMBER,
                    28
                )
            )

        val result =
            OnboardingDiscoveryEngine
                .salarySuggestion(
                    observations =
                        observations,
                    timeZone = zone,
                    excludedLabels =
                        setOf(
                            "PAYMENT GATEWAY LTD"
                        )
                )

        assertEquals(
            "REAL EMPLOYER LIMITED",
            result?.label
        )
    }

    private fun income(
        label: String,
        amountMinor: Long,
        year: Int,
        month: Int,
        day: Int
    ): OnboardingTransactionObservation {
        val calendar =
            Calendar.getInstance(
                TimeZone.getTimeZone(
                    "Africa/Nairobi"
                )
            ).apply {
                clear()
                set(
                    year,
                    month,
                    day,
                    12,
                    0,
                    0
                )
            }

        return OnboardingTransactionObservation(
            counterparty = label,
            counterpartyPhone = null,
            amountMinor = amountMinor,
            occurredAtEpochMs =
                calendar.timeInMillis,
            direction =
                MoneyDirection.INCOME,
            accountId = "account-bank"
        )
    }
}

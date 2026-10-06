package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiabilityPaymentLinkPolicyTest {
    @Test
    fun exactIdentityAndObservedOutstandingCanAutoLink() {
        val match =
            LiabilityPaymentLinkPolicy
                .chooseAutomatic(
                    counterparty =
                        "Branch International Ltd",
                    amountMinor = 400_000L,
                    candidates =
                        listOf(
                            candidate(
                                id = "debt-branch",
                                name = "Branch Loan",
                                providerKey =
                                    "discovered:branch",
                                outstandingMinor =
                                    460_700L,
                                observed = true
                            )
                        )
                )

        assertEquals(
            "debt-branch",
            match?.debtAccountId
        )
        assertEquals(
            RepaymentChainConfidence.HIGH,
            match?.confidence
        )
    }

    @Test
    fun identityMatchWithoutObservedOutstandingRequiresReview() {
        val matches =
            LiabilityPaymentLinkPolicy
                .candidates(
                    counterparty = "Future Lender Ltd",
                    amountMinor = 200_000L,
                    candidates =
                        listOf(
                            candidate(
                                id = "debt-future",
                                name = "Future Lender Loan",
                                providerKey =
                                    "discovered:future-lender",
                                outstandingMinor = 0L,
                                observed = false
                            )
                        )
                )

        assertEquals(1, matches.size)
        assertEquals(
            RepaymentChainConfidence.MEDIUM,
            matches.single().confidence
        )
        assertNull(
            LiabilityPaymentLinkPolicy
                .chooseAutomatic(
                    counterparty =
                        "Future Lender Ltd",
                    amountMinor = 200_000L,
                    candidates =
                        listOf(
                            candidate(
                                id = "debt-future",
                                name =
                                    "Future Lender Loan",
                                providerKey =
                                    "discovered:future-lender",
                                outstandingMinor = 0L,
                                observed = false
                            )
                        )
                )
        )
    }

    @Test
    fun paymentAboveObservedOutstandingRequiresReview() {
        val matches =
            LiabilityPaymentLinkPolicy
                .candidates(
                    counterparty = "Example Credit",
                    amountMinor = 900_000L,
                    candidates =
                        listOf(
                            candidate(
                                id = "debt-example",
                                name = "Example Loan",
                                providerKey =
                                    "discovered:example",
                                outstandingMinor =
                                    500_000L,
                                observed = true
                            )
                        )
                )

        assertEquals(
            RepaymentChainConfidence.MEDIUM,
            matches.single().confidence
        )
    }

    @Test
    fun genericWordsDoNotCreateProviderMatch() {
        val match =
            LiabilityPaymentLinkPolicy
                .chooseAutomatic(
                    counterparty =
                        "Financial Services Kenya Ltd",
                    amountMinor = 100_000L,
                    candidates =
                        listOf(
                            candidate(
                                id = "debt-example",
                                name =
                                    "Example Finance Loan",
                                providerKey =
                                    "discovered:example-finance",
                                outstandingMinor =
                                    500_000L,
                                observed = true
                            )
                        )
                )

        assertNull(match)
    }

    @Test
    fun ambiguousIdentityNeverAutoLinks() {
        val candidates =
            listOf(
                candidate(
                    id = "debt-one",
                    name = "Example Loan",
                    providerKey =
                        "discovered:example",
                    outstandingMinor = 500_000L,
                    observed = true
                ),
                candidate(
                    id = "debt-two",
                    name = "Example Credit Loan",
                    providerKey =
                        "discovered:example-credit",
                    outstandingMinor = 500_000L,
                    observed = true
                )
            )

        val matches =
            LiabilityPaymentLinkPolicy
                .candidates(
                    counterparty = "Example",
                    amountMinor = 100_000L,
                    candidates = candidates
                )

        assertEquals(2, matches.size)
        assertEquals(
            setOf(
                RepaymentChainConfidence.MEDIUM
            ),
            matches.map { it.confidence }.toSet()
        )
        assertNull(
            LiabilityPaymentLinkPolicy
                .chooseAutomatic(
                    counterparty = "Example",
                    amountMinor = 100_000L,
                    candidates = candidates
                )
        )
    }

    @Test fun providerOverlapIsNotARepayment() {
        val fuliza = candidate("fuliza", "Fuliza M-PESA", "SAFARICOM_FULIZA", 50000L, true)
        assertEquals(emptyList<LiabilityPaymentMatch>(), LiabilityPaymentLinkPolicy.candidates("Safaricom", 2000L, listOf(fuliza)))
        assertEquals(emptyList<LiabilityPaymentMatch>(), LiabilityPaymentLinkPolicy.candidates("M-PESA", 2000L, listOf(fuliza)))
        assertNull(LiabilityPaymentLinkPolicy.chooseAutomatic("Safaricom", 2000L, listOf(fuliza)))
    }

    private fun candidate(
        id: String,
        name: String,
        providerKey: String,
        outstandingMinor: Long,
        observed: Boolean
    ) =
        LiabilityPaymentCandidate(
            debtAccountId = id,
            displayName = name,
            providerKey = providerKey,
            outstandingMinor =
                outstandingMinor,
            hasObservedOutstanding =
                observed
        )
}


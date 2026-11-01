package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateAccountingAcceptanceGateTest {
    @Test
    fun onlyFixturesWithSharedReferencesAutoLink() {
        val fixtures =
            listOf(
                mirrorFixture(
                    name = "shared external reference",
                    first =
                        bank(
                            amount = 500_000L,
                            at = 1_000_000L
                        ).copy(
                            transactionCode =
                                "EQA12345",
                            externalReferences =
                                setOf(
                                    "EQA12345",
                                    "UFT98765"
                                )
                        ),
                    second =
                        mpesa(
                            amount = 500_000L,
                            at = 1_090_000L,
                            direction = "INCOME",
                            code = "UFT98765"
                        )
                ),
                mirrorFixture(
                    name = "same transaction code",
                    first =
                        mpesa(
                            amount = 100_000L,
                            at = 2_000_000L,
                            code = "ABC12345"
                        ),
                    second =
                        mpesa(
                            amount = 100_000L,
                            at = 2_040_000L,
                            code = "ABC12345",
                            sourcePackage =
                                "com.other.notification"
                        )
                ),
                mirrorFixture(
                    name = "complementary account path",
                    first =
                        bank(
                            amount = 200_000L,
                            at = 3_000_000L,
                            direction = "TRANSFER"
                        ).copy(
                            accountIds =
                                setOf(
                                    "account-bank",
                                    "account-mpesa"
                                )
                        ),
                    second =
                        mpesa(
                            amount = 200_000L,
                            at = 3_040_000L,
                            direction = "INCOME",
                            code = ""
                        ).copy(
                            transactionCode = null,
                            accountIds =
                                setOf(
                                    "account-mpesa"
                                )
                        )
                ),
                mirrorFixture(
                    name = "shared stable identity",
                    first =
                        bank(
                            amount = 85_000L,
                            at = 4_000_000L,
                            counterparty =
                                "Sample Shop"
                        ),
                    second =
                        mpesa(
                            amount = 85_000L,
                            at = 4_030_000L,
                            direction = "EXPENSE",
                            code = ""
                        ).copy(
                            transactionCode = null,
                            counterparty =
                                "SAMPLE SHOP"
                        )
                ),
                mirrorFixture(
                    name = "complementary directions",
                    first =
                        bank(
                            amount = 125_000L,
                            at = 5_000_000L,
                            direction = "TRANSFER"
                        ),
                    second =
                        mpesa(
                            amount = 125_000L,
                            at = 5_050_000L,
                            direction = "INCOME",
                            code = ""
                        ).copy(
                            transactionCode = null
                        )
                ),
                mirrorFixture(
                    name = "coded and codeless very close",
                    first =
                        mpesa(
                            amount = 98_500L,
                            at = 6_000_000L,
                            code = "TST12345"
                        ),
                    second =
                        bank(
                            amount = 98_500L,
                            at = 6_018_000L
                        )
                )
            )

        val referenceBacked = setOf("shared external reference", "same transaction code")
        fixtures.forEach { fixture ->
            val linked = MirrorTransactionMatcher.match(fixture.first, fixture.second).autoLink
            assertEquals("Unexpected automatic match for ${fixture.name}", fixture.name in referenceBacked, linked)
        }
    }

    @Test
    fun allKnownDistinctFixturesStaySeparate() {
        val fixtures =
            listOf(
                mirrorFixture(
                    name = "different amounts even with shared reference",
                    first =
                        bank(
                            amount = 500_000L,
                            at = 1_000_000L
                        ).copy(
                            externalReferences =
                                setOf(
                                    "REF12345"
                                )
                        ),
                    second =
                        mpesa(
                            amount = 700_000L,
                            at = 1_001_000L,
                            code = "REF12345"
                        )
                ),
                mirrorFixture(
                    name = "different real transaction codes",
                    first =
                        mpesa(
                            amount = 100_000L,
                            at = 2_000_000L,
                            code = "AAA11111"
                        ),
                    second =
                        mpesa(
                            amount = 100_000L,
                            at = 2_002_000L,
                            code = "BBB22222",
                            sourcePackage =
                                "com.other.source"
                        )
                ),
                mirrorFixture(
                    name = "far apart",
                    first =
                        bank(
                            amount = 100_000L,
                            at = 3_000_000L,
                            counterparty =
                                "KCB"
                        ),
                    second =
                        mpesa(
                            amount = 100_000L,
                            at = 3_300_001L,
                            counterparty =
                                "KCB",
                            code = ""
                        ).copy(
                            transactionCode = null
                        )
                ),
                mirrorFixture(
                    name = "reversal is not a mirror movement",
                    first =
                        bank(
                            amount = 100_000L,
                            at = 4_000_000L
                        ),
                    second =
                        mpesa(
                            amount = 100_000L,
                            at = 4_001_000L,
                            code = "REV12345"
                        ).copy(
                            type = "REVERSAL"
                        )
                ),
                mirrorFixture(
                    name = "same source codeless time-only similarity",
                    first =
                        bank(
                            amount = 100_000L,
                            at = 5_000_000L,
                            sourcePackage =
                                "com.bank.app"
                        ),
                    second =
                        bank(
                            amount = 100_000L,
                            at = 5_003_000L,
                            sourcePackage =
                                "com.bank.app"
                        )
                ),
                mirrorFixture(
                    name = "ultra-close codeless distinct-source pair remains review-only",
                    first =
                        bank(
                            amount = 100_000L,
                            at = 6_000_000L,
                            sourcePackage =
                                "com.bank.one"
                        ),
                    second =
                        bank(
                            amount = 100_000L,
                            at = 6_003_000L,
                            sourcePackage =
                                "com.bank.two"
                        )
                )
            )

        val falseLinks =
            fixtures.filter {
                MirrorTransactionMatcher
                    .match(
                        it.first,
                        it.second
                    )
                    .autoLink
            }

        assertTrue(
            "Known distinct fixtures auto-linked incorrectly: " +
                falseLinks.joinToString {
                    it.name
                },
            falseLinks.isEmpty()
        )
    }

    @Test
    fun ambiguousCodelessCrossSourceFixtureIsReviewableButNotAutoLinked() {
        val first =
            possibleDuplicate(
                id = 1L,
                amount = 98_500L,
                at = 1_000_000L,
                sourcePackage =
                    "com.bank.one"
            )
        val second =
            possibleDuplicate(
                id = 2L,
                amount = 98_500L,
                at = 1_012_000L,
                sourcePackage =
                    "com.bank.two"
            )

        assertTrue(
            PossibleDuplicatePolicy
                .isCandidate(
                    first,
                    second
                )
        )

        val mirrorMatch =
            MirrorTransactionMatcher
                .match(
                    bank(
                        amount = 98_500L,
                        at = 1_000_000L,
                        sourcePackage =
                            "com.bank.one"
                    ),
                    bank(
                        amount = 98_500L,
                        at = 1_012_000L,
                        sourcePackage =
                            "com.bank.two"
                    )
                )

        assertFalse(
            mirrorMatch.autoLink
        )
        assertEquals(
            MirrorMatchConfidence.NONE,
            mirrorMatch.confidence
        )
    }

    private fun mirrorFixture(
        name: String,
        first: MirrorTransactionObservation,
        second: MirrorTransactionObservation
    ) =
        MirrorFixture(
            name = name,
            first = first,
            second = second
        )

    private fun mpesa(
        amount: Long,
        at: Long,
        counterparty: String? = null,
        direction: String = "EXPENSE",
        code: String = "TQK12ABC34",
        sourcePackage: String =
            "com.safaricom.mpesa"
    ) =
        MirrorTransactionObservation(
            amountMinor = amount,
            occurredAtEpochMs = at,
            receivedAtEpochMs = at,
            sourceKind =
                "financial_notification",
            sourcePackage =
                sourcePackage,
            transactionCode =
                code,
            type = "PAYMENT",
            counterparty =
                counterparty,
            counterpartyPhone = null,
            direction =
                direction
        )

    private fun bank(
        amount: Long,
        at: Long,
        counterparty: String? = null,
        direction: String = "NEUTRAL",
        sourcePackage: String =
            "com.bank.app"
    ) =
        MirrorTransactionObservation(
            amountMinor = amount,
            occurredAtEpochMs = at,
            receivedAtEpochMs = at,
            sourceKind =
                "financial_notification",
            sourcePackage =
                sourcePackage,
            transactionCode = null,
            type = "OTHER",
            counterparty =
                counterparty,
            counterpartyPhone = null,
            direction =
                direction
        )

    private fun possibleDuplicate(
        id: Long,
        amount: Long,
        at: Long,
        sourcePackage: String
    ) =
        PossibleDuplicateObservation(
            id = id,
            amountMinor = amount,
            receivedAtEpochMs = at,
            transactionCode = null,
            sourcePackage =
                sourcePackage,
            sourceKind =
                "financial_notification",
            direction = "NEUTRAL",
            reviewState =
                "NEEDS_REVIEW",
            category =
                "UNCATEGORIZED",
            counterparty = null,
            evidenceLabel = null,
            type = "OTHER"
        )

    private data class MirrorFixture(
        val name: String,
        val first: MirrorTransactionObservation,
        val second: MirrorTransactionObservation
    )
}

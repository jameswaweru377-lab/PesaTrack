package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialMessageFixtureCorpusTest {
    @Test
    fun corpusLocksCurrentGenericCoverageAndFallbackBoundaries() {
        FinancialMessageFixtureCorpus.all.forEach { fixture ->
            val interpretation =
                FinancialMessageInterpreter.interpret(
                    title = fixture.title,
                    body = fixture.body
                )
            val canonical =
                interpretation.toCanonicalDraft()

            assertEquals(
                fixture.id + ": transaction code",
                fixture.transactionCode,
                canonical.transactionCode
            )
            assertEquals(
                fixture.id + ": canonical record kinds",
                fixture.expectedKinds,
                canonical.records.map { it.kind }
            )

            val balances = canonical.records
                .filterIsInstance<BalanceObservationDraft>()

            assertEquals(
                fixture.id + ": balance count",
                fixture.expectedBalances.size,
                balances.size
            )

            fixture.expectedBalances.forEach { expected ->
                val actual = balances.singleOrNull {
                    it.account.providerKey ==
                        expected.providerKey &&
                        it.account.displayName ==
                            expected.displayName
                }
                checkNotNull(actual) {
                    fixture.id +
                        ": missing expected balance for " +
                        expected.displayName
                }
                assertEquals(
                    fixture.id + ": " +
                        expected.displayName +
                        " balance",
                    expected.amountMinor,
                    actual.balanceMinor
                )
            }

            when (fixture.coverage) {
                FixtureCoverage.GENERIC_COMPLETE -> {
                    assertFalse(
                        fixture.id +
                            ": should not require legacy parsing",
                        canonical.requiresLegacyFallback
                    )
                    assertTrue(
                        fixture.id +
                            ": generic fixture needs records",
                        canonical.records.isNotEmpty()
                    )
                }
                FixtureCoverage.GENERIC_SEMANTICS_LEGACY_PERSISTENCE -> {
                    assertTrue(
                        fixture.id +
                            ": semantics are generic but persistence still falls back",
                        canonical.requiresLegacyFallback
                    )
                    assertEquals(
                        fixture.id +
                            ": generic debt semantics must match target",
                        fixture.targetKinds,
                        fixture.expectedKinds
                    )
                }
                FixtureCoverage.LEGACY_REQUIRED -> {
                    assertTrue(
                        fixture.id +
                            ": should document a legacy gap",
                        canonical.requiresLegacyFallback
                    )
                }
                FixtureCoverage.NON_FINANCIAL -> {
                    assertFalse(
                        fixture.id +
                            ": should remain non-financial",
                        canonical.recognized
                    )
                }
            }
        }
    }

    @Test
    fun corpusMakesMigrationTargetExplicit() {
        FinancialMessageFixtureCorpus.all.forEach { fixture ->
            when (fixture.coverage) {
                FixtureCoverage.GENERIC_COMPLETE ->
                    assertEquals(
                        fixture.id +
                            ": generic-complete fixture must already match its target",
                        fixture.targetKinds,
                        fixture.expectedKinds
                    )
                FixtureCoverage.GENERIC_SEMANTICS_LEGACY_PERSISTENCE ->
                    assertEquals(
                        fixture.id +
                            ": generic semantics must already match target",
                        fixture.targetKinds,
                        fixture.expectedKinds
                    )
                FixtureCoverage.LEGACY_REQUIRED -> {
                    assertTrue(
                        fixture.id +
                            ": legacy fixture needs a non-empty canonical target",
                        fixture.targetKinds.isNotEmpty()
                    )
                    assertTrue(
                        fixture.id +
                            ": legacy fixture should expose a remaining semantic gap",
                        fixture.targetKinds !=
                            fixture.expectedKinds
                    )
                }
                FixtureCoverage.NON_FINANCIAL ->
                    assertTrue(
                        fixture.id +
                            ": non-financial fixture must have no target records",
                        fixture.targetKinds.isEmpty()
                    )
            }
        }
    }

    @Test
    fun genericFixturesCarryFieldLevelEvidenceWithoutMessageCopies() {
        FinancialMessageFixtureCorpus.all
            .filter {
                it.coverage ==
                    FixtureCoverage.GENERIC_COMPLETE
            }
            .forEach { fixture ->
                val canonical =
                    FinancialMessageInterpreter
                        .interpret(
                            title = fixture.title,
                            body = fixture.body
                        )
                        .toCanonicalDraft()

                if (canonical.transactionCode != null) {
                    assertTrue(
                        fixture.id +
                            ": transaction code evidence missing",
                        canonical.metadataEvidence.any {
                            it.field ==
                                FinancialField.TRANSACTION_CODE &&
                                it.span != null
                        }
                    )
                }

                if (canonical.occurredAtText != null) {
                    assertTrue(
                        fixture.id +
                            ": time evidence missing",
                        canonical.metadataEvidence.any {
                            it.field ==
                                FinancialField.OCCURRED_AT &&
                                it.span != null
                        }
                    )
                }

                canonical.records
                    .filter {
                        it is MoneyMovementDraft ||
                            it is DebtMovementDraft ||
                            it is ReversalDraft
                    }
                    .forEach { record ->
                        assertTrue(
                            fixture.id +
                                ": amount evidence missing",
                            record.evidence.any {
                                it.field ==
                                    FinancialField.AMOUNT &&
                                    it.span != null
                            }
                        )

                        val hasRelationshipEvidence =
                            when (record) {
                                is MoneyMovementDraft ->
                                    record.evidence.any {
                                        it.field ==
                                            FinancialField.SOURCE_ACCOUNT ||
                                            it.field ==
                                                FinancialField.DESTINATION_ACCOUNT ||
                                            it.field ==
                                                FinancialField.COUNTERPARTY
                                    }
                                is DebtMovementDraft ->
                                    record.evidence.any {
                                        it.field ==
                                            FinancialField.LIABILITY
                                    }
                                is ReversalDraft ->
                                    record.evidence.any {
                                        it.field ==
                                            FinancialField
                                                .RELATED_TRANSACTION_CODE
                                    }
                                else -> false
                            }

                        assertTrue(
                            fixture.id +
                                ": relationship evidence missing",
                            hasRelationshipEvidence
                        )
                    }

                canonical.records
                    .filterIsInstance<BalanceObservationDraft>()
                    .forEach { balance ->
                        assertTrue(
                            fixture.id +
                                ": balance evidence missing",
                            balance.evidence.any {
                                it.field ==
                                    FinancialField.BALANCE &&
                                    it.span != null
                            }
                        )
                    }
            }
    }

    @Test
    fun corpusUsesSanitizedSyntheticIdentifiers() {
        FinancialMessageFixtureCorpus.all.forEach { fixture ->
            assertFalse(
                fixture.id + ": unexpected personal email",
                fixture.body.contains("@")
            )
            assertFalse(
                fixture.id + ": unexpected real-person marker",
                fixture.body.contains(
                    "JAMES",
                    ignoreCase = true
                )
            )
        }
    }
}

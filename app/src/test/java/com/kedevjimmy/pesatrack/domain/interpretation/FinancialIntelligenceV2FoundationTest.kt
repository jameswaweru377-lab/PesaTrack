package com.kedevjimmy.pesatrack.domain.interpretation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialIntelligenceV2FoundationTest {
    @Test
    fun corpusCurrencyVariantsNormalizeToCanonicalKes() {
        FinancialIntelligenceV2FoundationCorpus.all
            .forEach { fixture ->
                val normalized =
                    FinancialMessageNormalizer.normalize(
                        title = fixture.title,
                        body = fixture.body
                    )

                fixture.expectedNormalizedFragments
                    .forEach { fragment ->
                        assertTrue(
                            fixture.id +
                                " should contain " +
                                fragment +
                                " after normalization: " +
                                normalized.body,
                            normalized.body.contains(
                                fragment
                            )
                        )
                    }
            }
    }

    @Test
    fun relevanceGateMatchesSanitizedFoundationCorpus() {
        FinancialIntelligenceV2FoundationCorpus.all
            .forEach { fixture ->
                val normalized =
                    FinancialMessageNormalizer.normalize(
                        title = fixture.title,
                        body = fixture.body
                    )
                val relevance =
                    FinancialMessageRelevanceGate
                        .assess(normalized)

                assertEquals(
                    fixture.id +
                        " relevance mismatch; signals=" +
                        relevance.signals +
                        " score=" +
                        relevance.score,
                    fixture.expectedRelevant,
                    relevance.isRelevant
                )
            }
    }

    @Test
    fun existingFinancialCorpusStillPassesRelevanceGate() {
        FinancialMessageFixtureCorpus.all
            .filter {
                it.coverage !=
                    FixtureCoverage.NON_FINANCIAL
            }
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
                        " should remain financially relevant; " +
                        "signals=" +
                        relevance.signals +
                        " score=" +
                        relevance.score,
                    relevance.isRelevant
                )
            }
    }

    @Test
    fun existingNonFinancialCorpusIsRejectedByGate() {
        FinancialMessageFixtureCorpus.all
            .filter {
                it.coverage ==
                    FixtureCoverage.NON_FINANCIAL
            }
            .forEach { fixture ->
                val normalized =
                    FinancialMessageNormalizer.normalize(
                        title = fixture.title,
                        body = fixture.body
                    )
                val relevance =
                    FinancialMessageRelevanceGate
                        .assess(normalized)

                assertFalse(
                    fixture.id +
                        " should remain non-financial; " +
                        "signals=" +
                        relevance.signals +
                        " score=" +
                        relevance.score,
                    relevance.isRelevant
                )
            }
    }

    @Test
    fun providerNameAloneDoesNotMakeMessageFinancial() {
        val normalized =
            FinancialMessageNormalizer.normalize(
                title = "Future Bank",
                body =
                    "Welcome! Thank you for choosing us."
            )

        assertFalse(
            FinancialMessageRelevanceGate
                .assess(normalized)
                .isRelevant
        )
    }

    @Test
    fun unknownProviderCanPassUsingFinancialLanguageOnly() {
        val normalized =
            FinancialMessageNormalizer.normalize(
                title = "NewCo",
                body =
                    "Your loan of Shs 8,000 has been " +
                        "disbursed to your mobile wallet."
            )

        val relevance =
            FinancialMessageRelevanceGate
                .assess(normalized)

        assertTrue(relevance.isRelevant)
        assertTrue(
            normalized.body.contains("KES 8,000")
        )
    }
}

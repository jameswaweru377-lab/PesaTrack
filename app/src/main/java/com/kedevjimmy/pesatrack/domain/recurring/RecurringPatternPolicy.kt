package com.kedevjimmy.pesatrack.domain.recurring

import java.math.BigInteger
import kotlin.math.abs

object RecurringPatternPolicy {
    private const val DAY_MS =
        24L * 60L * 60L * 1000L

    const val MIN_OCCURRENCES = 3
    const val MIN_MONTHLY_INTERVAL_DAYS = 24
    const val MAX_MONTHLY_INTERVAL_DAYS = 38
    const val MAX_STALE_DAYS = 60

    /** Opt-in domain v2; the current UI and store still call [detect]. */
    fun detectV2(
        observations: List<RecurringPatternObservation>,
        nowEpochMs: Long
    ): List<RecurringPatternCandidate> =
        RecurringPatternV2Policy.detect(observations, nowEpochMs)

    fun detect(
        observations:
            List<RecurringPatternObservation>,
        nowEpochMs: Long
    ): List<RecurringPatternCandidate> =
        observations
            .groupBy {
                it.identityKey +
                    "|" +
                    it.categoryId
            }
            .mapNotNull {
                    (patternKey, raw) ->
                candidate(
                    patternKey =
                        patternKey,
                    observations = raw,
                    nowEpochMs =
                        nowEpochMs
                )
            }
            .sortedWith(
                compareByDescending<
                    RecurringPatternCandidate
                > {
                    confidenceRank(
                        it.confidence
                    )
                }.thenByDescending {
                    it.occurrenceCount
                }.thenBy {
                    it.nextExpectedAtEpochMs
                }
            )

    private fun candidate(
        patternKey: String,
        observations:
            List<RecurringPatternObservation>,
        nowEpochMs: Long
    ): RecurringPatternCandidate? {
        val ordered =
            observations
                .filter {
                    it.amountMinor > 0L
                }
                .sortedBy {
                    it.occurredAtEpochMs
                }

        if (
            ordered.size <
            MIN_OCCURRENCES
        ) {
            return null
        }

        val intervalsDays =
            ordered.zipWithNext {
                    first,
                    second ->
                (
                    (
                        second
                            .occurredAtEpochMs -
                            first
                                .occurredAtEpochMs
                    ).coerceAtLeast(0L) /
                        DAY_MS
                ).toInt()
            }

        if (intervalsDays.isEmpty()) {
            return null
        }

        val medianIntervalDays =
            medianInt(
                intervalsDays
            )
        if (
            medianIntervalDays !in
            MIN_MONTHLY_INTERVAL_DAYS..
                MAX_MONTHLY_INTERVAL_DAYS
        ) {
            return null
        }

        // Require every observed gap to stay near a
        // monthly cadence. This deliberately rejects
        // merchants used frequently throughout a month.
        if (
            intervalsDays.any {
                it !in
                    MIN_MONTHLY_INTERVAL_DAYS..
                        MAX_MONTHLY_INTERVAL_DAYS
            }
        ) {
            return null
        }

        val amounts =
            ordered.map {
                it.amountMinor
            }
        val typicalAmount =
            medianLong(amounts)
        if (typicalAmount <= 0L) {
            return null
        }

        val maximumAmountDeviation =
            amounts.maxOf {
                abs(it - typicalAmount)
            }
        // A recurring commitment should be useful for
        // planning. Very volatile amounts remain ordinary
        // history instead of becoming a guessed budget.
        if (!withinDeviationPercent(
                maximumAmountDeviation,
                typicalAmount,
                35L
            )
        ) {
            return null
        }

        val last =
            ordered.last()
        val ageDays =
            (
                (
                    nowEpochMs -
                        last
                            .occurredAtEpochMs
                ).coerceAtLeast(0L) /
                    DAY_MS
            ).toInt()
        if (ageDays > MAX_STALE_DAYS) {
            return null
        }

        val intervalSpread =
            (
                intervalsDays.maxOrNull()!! -
                    intervalsDays.minOrNull()!!
            )

        val confidence =
            if (
                ordered.size >= 4 &&
                withinDeviationPercent(
                    maximumAmountDeviation,
                    typicalAmount,
                    10L
                ) &&
                medianIntervalDays in
                    27..33 &&
                intervalSpread <= 6
            ) {
                RecurringPatternConfidence.HIGH
            } else {
                RecurringPatternConfidence.MEDIUM
            }

        return RecurringPatternCandidate(
            patternKey = patternKey,
            identityKey =
                last.identityKey,
            label = last.label,
            categoryId =
                last.categoryId,
            typicalAmountMinor =
                typicalAmount,
            occurrenceCount =
                ordered.size,
            medianIntervalDays =
                medianIntervalDays,
            lastObservedAtEpochMs =
                last.occurredAtEpochMs,
            nextExpectedAtEpochMs =
                last.occurredAtEpochMs +
                    medianIntervalDays *
                    DAY_MS,
            confidence = confidence,
            representativeTransactionId =
                last.transactionId,
            counterparty =
                last.counterparty,
            counterpartyPhone =
                last.counterpartyPhone,
            accountId =
                last.accountId
        )
    }

    private fun medianInt(
        values: List<Int>
    ): Int {
        val sorted =
            values.sorted()
        val middle =
            sorted.size / 2
        return if (
            sorted.size % 2 == 1
        ) {
            sorted[middle]
        } else {
            (
                sorted[middle - 1] +
                    sorted[middle]
            ) / 2
        }
    }

    private fun medianLong(
        values: List<Long>
    ): Long {
        val sorted =
            values.sorted()
        val middle =
            sorted.size / 2
        return if (
            sorted.size % 2 == 1
        ) {
            sorted[middle]
        } else {
            (
                sorted[middle - 1] +
                    sorted[middle]
            ) / 2L
        }
    }

    private fun withinDeviationPercent(
        deviation: Long,
        typicalAmount: Long,
        percent: Long
    ): Boolean =
        BigInteger.valueOf(deviation)
            .multiply(BigInteger.valueOf(100L)) <=
            BigInteger.valueOf(typicalAmount)
                .multiply(BigInteger.valueOf(percent))

    private fun confidenceRank(
        value: RecurringPatternConfidence
    ): Int =
        when (value) {
            RecurringPatternConfidence.HIGH ->
                2
            RecurringPatternConfidence.MEDIUM ->
                1
        }
}

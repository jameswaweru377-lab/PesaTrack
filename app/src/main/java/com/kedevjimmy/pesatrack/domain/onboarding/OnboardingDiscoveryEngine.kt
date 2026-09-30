package com.kedevjimmy.pesatrack.domain.onboarding

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.rules.OwnAccountTransferDetector
import java.util.Calendar
import java.util.TimeZone

data class OnboardingTransactionObservation(
    val counterparty: String?,
    val counterpartyPhone: String?,
    val amountMinor: Long,
    val occurredAtEpochMs: Long,
    val direction: MoneyDirection,
    val accountId: String?
)

data class OnboardingSalarySuggestion(
    val label: String,
    val accountId: String,
    val occurrenceCount: Int,
    val monthCount: Int,
    val representativeAmountMinor: Long
)

/**
 * Conservative first-run discovery.
 *
 * The onboarding flow should infer what it safely can from the same
 * provider-neutral ledger PesaTrack already builds, then ask the user
 * only to confirm ambiguous ownership or salary meaning.
 */
object OnboardingDiscoveryEngine {
    fun salarySuggestion(
        observations: List<OnboardingTransactionObservation>,
        timeZone: TimeZone = TimeZone.getDefault()
    ): OnboardingSalarySuggestion? {
        val candidates =
            observations
                .asSequence()
                .filter {
                    it.direction ==
                        MoneyDirection.INCOME &&
                        it.amountMinor >=
                            MIN_AUTOMATIC_SALARY_MINOR &&
                        !it.counterparty
                            .isNullOrBlank() &&
                        !it.accountId
                            .isNullOrBlank()
                }
                .groupBy {
                    normalizeLabel(
                        it.counterparty
                            .orEmpty()
                    ) +
                        "|" +
                        it.accountId
                            .orEmpty()
                }
                .values
                .mapNotNull { group ->
                    val byMonth =
                        group.groupBy {
                            monthKey(
                                it.occurredAtEpochMs,
                                timeZone
                            )
                        }

                    if (
                        byMonth.size < 2 ||
                        byMonth.values.any {
                            it.size >
                                MAX_SALARY_CREDITS_PER_MONTH
                        }
                    ) {
                        return@mapNotNull null
                    }

                    val representativeByMonth =
                        byMonth.values
                            .mapNotNull { month ->
                                month.maxByOrNull {
                                    it.amountMinor
                                }
                            }
                            .sortedBy {
                                it.occurredAtEpochMs
                            }

                    val gaps =
                        representativeByMonth
                            .zipWithNext {
                                    first,
                                    second ->
                                (
                                    second.occurredAtEpochMs -
                                        first.occurredAtEpochMs
                                    ) /
                                    DAY_MS
                            }

                    if (
                        gaps.isNotEmpty() &&
                        gaps.any {
                            it <
                                MIN_MONTHLY_GAP_DAYS ||
                                it >
                                MAX_MONTHLY_GAP_DAYS
                        }
                    ) {
                        return@mapNotNull null
                    }

                    val amounts =
                        representativeByMonth
                            .map {
                                it.amountMinor
                            }
                    val minAmount =
                        amounts.minOrNull()
                            ?: return@mapNotNull null
                    val maxAmount =
                        amounts.maxOrNull()
                            ?: return@mapNotNull null

                    if (
                        minAmount * 100L <
                        maxAmount *
                            MIN_AMOUNT_STABILITY_PERCENT
                    ) {
                        return@mapNotNull null
                    }

                    val latest =
                        representativeByMonth
                            .lastOrNull()
                            ?: return@mapNotNull null
                    val accountId =
                        latest.accountId
                            ?: return@mapNotNull null

                    OnboardingSalarySuggestion(
                        label =
                            latest.counterparty
                                ?.trim()
                                .orEmpty(),
                        accountId = accountId,
                        occurrenceCount =
                            representativeByMonth
                                .size,
                        monthCount =
                            representativeByMonth
                                .size,
                        representativeAmountMinor =
                            latest.amountMinor
                    )
                }

        return candidates.maxWithOrNull(
            compareBy<
                OnboardingSalarySuggestion
            >(
                { it.monthCount },
                { it.occurrenceCount },
                {
                    it.representativeAmountMinor
                }
            )
        )
    }

    fun mpesaPhoneSuggestion(
        observations: List<OnboardingTransactionObservation>,
        mpesaAccountId: String
    ): String? {
        val counts =
            observations
                .asSequence()
                .filter {
                    it.accountId != null &&
                        it.accountId != mpesaAccountId &&
                        it.direction in setOf(
                            MoneyDirection.EXPENSE,
                            MoneyDirection.TRANSFER
                        )
                }
                .mapNotNull {
                    it.counterpartyPhone
                        ?.let(
                            OwnAccountTransferDetector::
                                normalizeKenyanPhone
                        )
                }
                .groupingBy { it }
                .eachCount()

        val strongest =
            counts.entries.maxWithOrNull(
                compareBy<Map.Entry<String, Int>>(
                    { it.value },
                    { it.key }
                )
            ) ?: return null

        // Ownership must still be confirmed by the user. Requiring the
        // same destination twice keeps the prompt from appearing for a
        // random one-off recipient.
        return strongest.key
            .takeIf { strongest.value >= 2 }
    }

    private fun normalizeLabel(
        value: String
    ): String =
        value.uppercase()
            .replace(
                Regex("""[^A-Z0-9]+"""),
                " "
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()

    private const val
        MIN_AUTOMATIC_SALARY_MINOR =
        5_000_00L
    private const val
        MAX_SALARY_CREDITS_PER_MONTH =
        2
    private const val
        MIN_MONTHLY_GAP_DAYS =
        20L
    private const val
        MAX_MONTHLY_GAP_DAYS =
        45L
    private const val
        MIN_AMOUNT_STABILITY_PERCENT =
        55L
    private const val DAY_MS =
        24L * 60L * 60L * 1000L

    private fun monthKey(
        epochMs: Long,
        timeZone: TimeZone
    ): String {
        val calendar =
            Calendar.getInstance(timeZone)
                .apply {
                    timeInMillis = epochMs
                }
        return calendar.get(Calendar.YEAR)
            .toString() +
            "-" +
            calendar.get(Calendar.MONTH)
    }
}

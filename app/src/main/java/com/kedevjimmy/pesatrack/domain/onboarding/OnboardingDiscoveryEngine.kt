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
        timeZone: TimeZone = TimeZone.getDefault(),
        excludedLabels: Set<String> =
            emptySet()
    ): OnboardingSalarySuggestion? {
        val candidates =
            observations
                .asSequence()
                .filter {
                    it.direction == MoneyDirection.INCOME &&
                        it.amountMinor > 0L &&
                        !it.counterparty.isNullOrBlank() &&
                        !it.accountId.isNullOrBlank()
                }
                .filterNot {
                    normalizeLabel(
                        it.counterparty.orEmpty()
                    ) in excludedLabels
                        .map(::normalizeLabel)
                        .toSet()
                }
                .groupBy {
                    normalizeLabel(it.counterparty.orEmpty())
                }
                .values
                .mapNotNull { group ->
                    val months =
                        group.map {
                            monthKey(
                                it.occurredAtEpochMs,
                                timeZone
                            )
                        }.toSet()

                    // A single incoming payment is not enough evidence
                    // to label something as salary during onboarding.
                    if (
                        group.size < 2 ||
                        months.size < 2
                    ) {
                        return@mapNotNull null
                    }

                    val latest =
                        group.maxByOrNull {
                            it.occurredAtEpochMs
                        } ?: return@mapNotNull null
                    val accountId =
                        latest.accountId
                            ?: return@mapNotNull null

                    OnboardingSalarySuggestion(
                        label =
                            latest.counterparty
                                ?.trim()
                                .orEmpty(),
                        accountId = accountId,
                        occurrenceCount = group.size,
                        monthCount = months.size,
                        representativeAmountMinor =
                            latest.amountMinor
                    )
                }

        return candidates.maxWithOrNull(
            compareBy<OnboardingSalarySuggestion>(
                { it.monthCount },
                { it.occurrenceCount },
                { it.representativeAmountMinor }
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

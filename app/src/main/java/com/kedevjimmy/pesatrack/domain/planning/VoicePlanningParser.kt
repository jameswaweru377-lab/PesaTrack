package com.kedevjimmy.pesatrack.domain.planning

import com.kedevjimmy.pesatrack.planning.BillCadence
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

enum class VoicePlanKind {
    GOAL,
    BILL
}

sealed interface VoicePlanDraft {
    val sourceText: String
}

data class VoiceGoalDraft(
    override val sourceText: String,
    val name: String,
    val totalTargetMinor: Long,
    val targetDateEpochMs: Long?,
    val monthlyContributionMinor: Long
) : VoicePlanDraft

data class VoiceBillDraft(
    override val sourceText: String,
    val name: String,
    val amountMinor: Long,
    val payee: String,
    val cadence: BillCadence,
    val dueDay: Int?,
    val dueDayOfWeek: Int?
) : VoicePlanDraft

data class VoicePlanParseResult(
    val draft: VoicePlanDraft? = null,
    val error: String? = null
)

object VoicePlanningParser {
    fun parse(
        kind: VoicePlanKind,
        transcript: String,
        nowEpochMs: Long =
            System.currentTimeMillis(),
        locale: Locale =
            Locale.getDefault()
    ): VoicePlanParseResult {
        val text =
            transcript.trim()
        if (text.isBlank()) {
            return VoicePlanParseResult(
                error =
                    "I didn't hear a command. Try again."
            )
        }

        return when (kind) {
            VoicePlanKind.GOAL ->
                parseGoal(
                    text,
                    nowEpochMs,
                    locale
                )
            VoicePlanKind.BILL ->
                parseBill(
                    text,
                    nowEpochMs,
                    locale
                )
        }
    }

    private fun parseGoal(
        text: String,
        nowEpochMs: Long,
        locale: Locale
    ): VoicePlanParseResult {
        val amountMinor =
            parseMoneyMinor(text)
                ?: return VoicePlanParseResult(
                    error =
                        "Say the goal amount, for example “target 45,000”."
                )
        val targetDate =
            parseTargetDate(
                text,
                nowEpochMs,
                locale
            )
                ?: return VoicePlanParseResult(
                    error =
                        "Say the target date too, for example “by 8 February”."
                )
        val name =
            parseGoalName(text)
                .ifBlank {
                    "Savings goal"
                }
        val months =
            monthsUntil(
                nowEpochMs,
                targetDate
            )
        val monthly =
            ceilDiv(
                amountMinor,
                months.toLong()
            )

        return VoicePlanParseResult(
            draft =
                VoiceGoalDraft(
                    sourceText = text,
                    name = name,
                    totalTargetMinor =
                        amountMinor,
                    targetDateEpochMs =
                        targetDate,
                    monthlyContributionMinor =
                        monthly
                )
        )
    }

    private fun parseBill(
        text: String,
        nowEpochMs: Long,
        locale: Locale
    ): VoicePlanParseResult {
        val amountMinor =
            parseMoneyMinor(text)
                ?: return VoicePlanParseResult(
                    error =
                        "Say the bill amount, for example “WiFi bill 1,500”."
                )
        val name =
            parseBillName(text)
        if (name.isBlank()) {
            return VoicePlanParseResult(
                error =
                    "Say the bill name too, for example “WiFi bill 1,500 due on the 5th”."
            )
        }

        val lower =
            text.lowercase(locale)
        val cadence =
            if (
                "weekly" in lower ||
                "every week" in lower ||
                "each week" in lower
            ) {
                BillCadence.WEEKLY
            } else {
                BillCadence.MONTHLY
            }

        val dueDayOfWeek =
            if (
                cadence ==
                BillCadence.WEEKLY
            ) {
                parseWeekday(lower)
                    ?: return VoicePlanParseResult(
                        error =
                            "Say the weekly due day, for example “every Friday”."
                    )
            } else {
                null
            }

        val dueDay =
            if (
                cadence ==
                BillCadence.MONTHLY
            ) {
                parseMonthlyDueDay(
                    lower
                )
                    ?: return VoicePlanParseResult(
                        error =
                            "Say the monthly due day, for example “due on the 5th”."
                    )
            } else {
                null
            }

        return VoicePlanParseResult(
            draft =
                VoiceBillDraft(
                    sourceText = text,
                    name = name,
                    amountMinor =
                        amountMinor,
                    payee = name,
                    cadence = cadence,
                    dueDay = dueDay,
                    dueDayOfWeek =
                        dueDayOfWeek
                )
        )
    }

    internal fun parseMoneyMinor(
        raw: String
    ): Long? {
        val lower =
            raw.lowercase(
                Locale.ENGLISH
            )

        val strongPatterns =
            listOf(
                Regex(
                    """(?:kes|kshs?|shillings?)\s*([0-9][0-9,]*(?:\.\d+)?)\s*(k|thousand|m|million)?"""
                ),
                Regex(
                    """(?:target(?:\s+of)?|amount(?:\s+of)?|bill(?:\s+of)?|worth|save|saving|cost(?:s)?|price(?:\s+of)?)\s*(?:kes|kshs?|shillings?)?\s*([0-9][0-9,]*(?:\.\d+)?)\s*(k|thousand|m|million)?"""
                )
            )

        strongPatterns.forEach {
            pattern ->
            pattern.find(lower)
                ?.let {
                    match ->
                    parseNumberWithSuffix(
                        match.groupValues[1],
                        match.groupValues
                            .getOrNull(2)
                    )
                        ?.takeIf {
                            it > 0L
                        }
                        ?.let {
                            return it * 100L
                        }
                }
        }

        val candidates =
            Regex(
                """([0-9][0-9,]*(?:\.\d+)?)\s*(k|thousand|m|million)?"""
            )
                .findAll(lower)
                .mapNotNull {
                    match ->
                    val value =
                        parseNumberWithSuffix(
                            match.groupValues[1],
                            match.groupValues
                                .getOrNull(2)
                        ) ?: return@mapNotNull null
                    if (
                        value in 1900L..2100L ||
                        value <= 31L
                    ) {
                        null
                    } else {
                        value
                    }
                }
                .toList()

        return candidates
            .maxOrNull()
            ?.times(100L)
    }

    private fun parseNumberWithSuffix(
        number: String,
        suffix: String?
    ): Long? {
        val numeric =
            number.replace(",", "")
                .toDoubleOrNull()
                ?: return null
        val multiplier =
            when (
                suffix.orEmpty()
                    .lowercase()
            ) {
                "k",
                "thousand" ->
                    1_000.0
                "m",
                "million" ->
                    1_000_000.0
                else ->
                    1.0
            }

        return (numeric * multiplier)
            .toLong()
    }

    private fun parseGoalName(
        raw: String
    ): String {
        val text =
            raw.trim()

        val beforeGoal =
            Regex(
                """(?i)\b(?:add|create|set|make)\s+(?:a\s+|an\s+|my\s+)?(.+?)\s+goal\b"""
            )
                .find(text)
                ?.groupValues
                ?.getOrNull(1)
                ?.let(::cleanName)
                .orEmpty()
        if (
            beforeGoal.isNotBlank() &&
            beforeGoal.lowercase() !in
            setOf(
                "saving",
                "savings",
                "new"
            )
        ) {
            return beforeGoal
        }

        val afterGoal =
            Regex(
                """(?i)\bgoal\s+(?:for|called|named)\s+(.+?)(?=\s+(?:target|worth|by|before|due|of\s+(?:kes|ksh|[0-9]))\b|$)"""
            )
                .find(text)
                ?.groupValues
                ?.getOrNull(1)
                ?.let(::cleanName)
                .orEmpty()

        return afterGoal
    }

    private fun parseBillName(
        raw: String
    ): String {
        val text =
            raw.trim()

        Regex(
            """(?i)\b(?:add|create|set|make)\s+(?:a\s+|an\s+|my\s+)?(.+?)\s+bill\b"""
        )
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::cleanName)
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {
                return it
            }

        Regex(
            """(?i)\bbill\s+(?:for|called|named|of)\s+(.+?)(?=\s+(?:amount|due|every|monthly|weekly|on\s+the|of\s+(?:kes|ksh|[0-9])|[0-9])\b|$)"""
        )
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::cleanName)
            ?.takeIf {
                it.isNotBlank() &&
                    it.firstOrNull()
                        ?.isDigit() !=
                    true
            }
            ?.let {
                return it
            }

        return ""
    }

    private fun cleanName(
        raw: String
    ): String =
        raw.trim()
            .trim(
                ',',
                '.',
                '-',
                ':'
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .replaceFirstChar {
                if (
                    it.isLowerCase()
                ) {
                    it.titlecase()
                } else {
                    it.toString()
                }
            }

    private fun parseTargetDate(
        raw: String,
        nowEpochMs: Long,
        locale: Locale
    ): Long? {
        val lower =
            raw.lowercase(locale)
        val months =
            monthAliases()

        val dayFirst =
            Regex(
                """\b([0-3]?[0-9])(?:st|nd|rd|th)?(?:\s+of)?\s+([a-z]+)(?:\s+([0-9]{4}))?\b"""
            )
                .findAll(lower)
                .firstOrNull {
                    months.containsKey(
                        it.groupValues[2]
                    )
                }
        if (dayFirst != null) {
            val day =
                dayFirst.groupValues[1]
                    .toIntOrNull()
                    ?: return null
            val month =
                months[
                    dayFirst.groupValues[2]
                ] ?: return null
            val year =
                dayFirst.groupValues
                    .getOrNull(3)
                    ?.toIntOrNull()
            return futureDate(
                day,
                month,
                year,
                nowEpochMs
            )
        }

        val monthFirst =
            Regex(
                """\b([a-z]+)\s+([0-3]?[0-9])(?:st|nd|rd|th)?(?:\s*,?\s*([0-9]{4}))?\b"""
            )
                .findAll(lower)
                .firstOrNull {
                    months.containsKey(
                        it.groupValues[1]
                    )
                }
        if (monthFirst != null) {
            val month =
                months[
                    monthFirst.groupValues[1]
                ] ?: return null
            val day =
                monthFirst.groupValues[2]
                    .toIntOrNull()
                    ?: return null
            val year =
                monthFirst.groupValues
                    .getOrNull(3)
                    ?.toIntOrNull()
            return futureDate(
                day,
                month,
                year,
                nowEpochMs
            )
        }

        val endOfMonth =
            Regex(
                """\bend\s+of\s+([a-z]+)(?:\s+([0-9]{4}))?\b"""
            )
                .find(lower)
                ?: return null
        val month =
            months[
                endOfMonth.groupValues[1]
            ] ?: return null
        val explicitYear =
            endOfMonth.groupValues
                .getOrNull(2)
                ?.toIntOrNull()

        val seed =
            futureDate(
                1,
                month,
                explicitYear,
                nowEpochMs
            )
                ?: return null
        return Calendar.getInstance()
            .apply {
                timeInMillis = seed
                set(
                    Calendar.DAY_OF_MONTH,
                    getActualMaximum(
                        Calendar.DAY_OF_MONTH
                    )
                )
            }
            .timeInMillis
    }

    private fun futureDate(
        day: Int,
        month: Int,
        explicitYear: Int?,
        nowEpochMs: Long
    ): Long? {
        if (
            day !in 1..31
        ) {
            return null
        }
        val now =
            Calendar.getInstance()
                .apply {
                    timeInMillis =
                        nowEpochMs
                }
        var year =
            explicitYear
                ?: now.get(
                    Calendar.YEAR
                )

        fun build(
            selectedYear: Int
        ): Calendar =
            Calendar.getInstance()
                .apply {
                    clear()
                    set(
                        Calendar.YEAR,
                        selectedYear
                    )
                    set(
                        Calendar.MONTH,
                        month
                    )
                    set(
                        Calendar.DAY_OF_MONTH,
                        1
                    )
                    set(
                        Calendar.HOUR_OF_DAY,
                        23
                    )
                    set(
                        Calendar.MINUTE,
                        59
                    )
                    set(
                        Calendar.SECOND,
                        0
                    )
                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                    set(
                        Calendar.DAY_OF_MONTH,
                        minOf(
                            day,
                            getActualMaximum(
                                Calendar.DAY_OF_MONTH
                            )
                        )
                    )
                }

        var target =
            build(year)
        if (
            explicitYear == null &&
            target.timeInMillis <=
            nowEpochMs
        ) {
            year += 1
            target = build(year)
        }

        return target.timeInMillis
    }

    private fun monthAliases():
        Map<String, Int> =
        mapOf(
            "january" to
                Calendar.JANUARY,
            "jan" to
                Calendar.JANUARY,
            "february" to
                Calendar.FEBRUARY,
            "feb" to
                Calendar.FEBRUARY,
            "march" to
                Calendar.MARCH,
            "mar" to
                Calendar.MARCH,
            "april" to
                Calendar.APRIL,
            "apr" to
                Calendar.APRIL,
            "may" to
                Calendar.MAY,
            "june" to
                Calendar.JUNE,
            "jun" to
                Calendar.JUNE,
            "july" to
                Calendar.JULY,
            "jul" to
                Calendar.JULY,
            "august" to
                Calendar.AUGUST,
            "aug" to
                Calendar.AUGUST,
            "september" to
                Calendar.SEPTEMBER,
            "sep" to
                Calendar.SEPTEMBER,
            "sept" to
                Calendar.SEPTEMBER,
            "october" to
                Calendar.OCTOBER,
            "oct" to
                Calendar.OCTOBER,
            "november" to
                Calendar.NOVEMBER,
            "nov" to
                Calendar.NOVEMBER,
            "december" to
                Calendar.DECEMBER,
            "dec" to
                Calendar.DECEMBER
        )

    private fun parseMonthlyDueDay(
        lower: String
    ): Int? {
        val patterns =
            listOf(
                Regex(
                    """\bdue(?:\s+on)?(?:\s+the)?\s+([0-3]?[0-9])(?:st|nd|rd|th)?\b"""
                ),
                Regex(
                    """\bevery\s+month(?:\s+on)?(?:\s+the)?\s+([0-3]?[0-9])(?:st|nd|rd|th)?\b"""
                ),
                Regex(
                    """\bmonthly(?:\s+on)?(?:\s+the)?\s+([0-3]?[0-9])(?:st|nd|rd|th)?\b"""
                )
            )

        return patterns.asSequence()
            .mapNotNull {
                it.find(lower)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
            }
            .firstOrNull {
                it in 1..31
            }
    }

    private fun parseWeekday(
        lower: String
    ): Int? =
        when {
            "monday" in lower ->
                Calendar.MONDAY
            "tuesday" in lower ->
                Calendar.TUESDAY
            "wednesday" in lower ->
                Calendar.WEDNESDAY
            "thursday" in lower ->
                Calendar.THURSDAY
            "friday" in lower ->
                Calendar.FRIDAY
            "saturday" in lower ->
                Calendar.SATURDAY
            "sunday" in lower ->
                Calendar.SUNDAY
            else ->
                null
        }

    private fun monthsUntil(
        nowEpochMs: Long,
        targetEpochMs: Long
    ): Int {
        val delta =
            (targetEpochMs -
                nowEpochMs)
                .coerceAtLeast(1L)
        val days =
            delta.toDouble() /
                TimeUnit.DAYS
                    .toMillis(1)
                    .toDouble()
        return ceil(
            days / 30.4375
        ).toInt()
            .coerceAtLeast(1)
    }

    private fun ceilDiv(
        value: Long,
        divisor: Long
    ): Long =
        (value + divisor - 1L) /
            divisor
}

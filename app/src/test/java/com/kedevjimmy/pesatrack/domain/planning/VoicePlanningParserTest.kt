package com.kedevjimmy.pesatrack.domain.planning

import com.kedevjimmy.pesatrack.planning.BillCadence
import java.util.Calendar
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoicePlanningParserTest {
    @Test
    fun goalCommandBuildsConfirmationDraft() {
        val now =
            calendar(
                year = 2026,
                month = Calendar.OCTOBER,
                day = 8
            )

        val result =
            VoicePlanningParser.parse(
                kind = VoicePlanKind.GOAL,
                transcript =
                    "Add a PS5 goal with a target of 45,000 by 8th of February",
                nowEpochMs =
                    now.timeInMillis,
                locale = Locale.ENGLISH
            )

        assertNull(result.error)
        val draft =
            result.draft as
                VoiceGoalDraft
        assertEquals(
            "PS5",
            draft.name
        )
        assertEquals(
            4_500_000L,
            draft.totalTargetMinor
        )
        assertEquals(
            1_125_000L,
            draft.monthlyContributionMinor
        )

        val target =
            Calendar.getInstance()
                .apply {
                    timeInMillis =
                        requireNotNull(
                            draft.targetDateEpochMs
                        )
                }
        assertEquals(
            2027,
            target.get(
                Calendar.YEAR
            )
        )
        assertEquals(
            Calendar.FEBRUARY,
            target.get(
                Calendar.MONTH
            )
        )
        assertEquals(
            8,
            target.get(
                Calendar.DAY_OF_MONTH
            )
        )
    }

    @Test
    fun naturalBuyPhraseExtractsCouchAsGoalName() {
        val result =
            VoicePlanningParser.parse(
                kind = VoicePlanKind.GOAL,
                transcript =
                    "I wanted to buy a couch for 60,000 by 20 December",
                nowEpochMs =
                    calendar(
                        2026,
                        Calendar.OCTOBER,
                        8
                    ).timeInMillis,
                locale = Locale.ENGLISH
            )

        assertNull(result.error)
        val draft =
            result.draft as
                VoiceGoalDraft
        assertEquals(
            "Couch",
            draft.name
        )
        assertEquals(
            6_000_000L,
            draft.totalTargetMinor
        )
    }

    @Test
    fun naturalSavingPhraseExtractsPs5AsGoalName() {
        val result =
            VoicePlanningParser.parse(
                kind = VoicePlanKind.GOAL,
                transcript =
                    "I'm targeting to save for a PS5 with a target of 45,000 by 8 February",
                nowEpochMs =
                    calendar(
                        2026,
                        Calendar.OCTOBER,
                        8
                    ).timeInMillis,
                locale = Locale.ENGLISH
            )

        assertNull(result.error)
        val draft =
            result.draft as
                VoiceGoalDraft
        assertEquals(
            "PS5",
            draft.name
        )
        assertEquals(
            4_500_000L,
            draft.totalTargetMinor
        )
    }

    @Test
    fun savingTowardsPhraseExtractsItemName() {
        val result =
            VoicePlanningParser.parse(
                kind = VoicePlanKind.GOAL,
                transcript =
                    "I am saving towards a new sofa target 80,000 by 15 March",
                nowEpochMs =
                    calendar(
                        2026,
                        Calendar.OCTOBER,
                        8
                    ).timeInMillis,
                locale = Locale.ENGLISH
            )

        assertNull(result.error)
        val draft =
            result.draft as
                VoiceGoalDraft
        assertEquals(
            "Sofa",
            draft.name
        )
    }

    @Test
    fun monthlyBillCommandBuildsDueSchedule() {
        val result =
            VoicePlanningParser.parse(
                kind = VoicePlanKind.BILL,
                transcript =
                    "Add WiFi bill of 1,500 every month due on the 5th",
                nowEpochMs =
                    calendar(
                        2026,
                        Calendar.OCTOBER,
                        8
                    ).timeInMillis,
                locale = Locale.ENGLISH
            )

        assertNull(result.error)
        val draft =
            result.draft as
                VoiceBillDraft
        assertEquals(
            "WiFi",
            draft.name
        )
        assertEquals(
            150_000L,
            draft.amountMinor
        )
        assertEquals(
            BillCadence.MONTHLY,
            draft.cadence
        )
        assertEquals(
            5,
            draft.dueDay
        )
    }

    @Test
    fun weeklyBillRequiresAndParsesWeekday() {
        val result =
            VoicePlanningParser.parse(
                kind = VoicePlanKind.BILL,
                transcript =
                    "Add gym bill 800 every week on Friday",
                nowEpochMs =
                    calendar(
                        2026,
                        Calendar.OCTOBER,
                        8
                    ).timeInMillis,
                locale = Locale.ENGLISH
            )

        assertNull(result.error)
        val draft =
            result.draft as
                VoiceBillDraft
        assertEquals(
            BillCadence.WEEKLY,
            draft.cadence
        )
        assertEquals(
            Calendar.FRIDAY,
            draft.dueDayOfWeek
        )
    }

    @Test
    fun incompleteCommandIsNotSavedBlindly() {
        val result =
            VoicePlanningParser.parse(
                kind = VoicePlanKind.GOAL,
                transcript =
                    "Add a goal target 45,000",
                nowEpochMs =
                    calendar(
                        2026,
                        Calendar.OCTOBER,
                        8
                    ).timeInMillis,
                locale = Locale.ENGLISH
            )

        assertNull(result.draft)
        assertNotNull(result.error)
        assertTrue(
            result.error!!
                .contains(
                    "target date",
                    ignoreCase = true
                )
        )
    }

    private fun calendar(
        year: Int,
        month: Int,
        day: Int
    ): Calendar =
        Calendar.getInstance()
            .apply {
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
}

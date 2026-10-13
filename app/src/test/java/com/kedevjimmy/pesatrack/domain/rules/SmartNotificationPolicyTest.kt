package com.kedevjimmy.pesatrack.domain.rules

import org.junit.Assert.*
import org.junit.Test

class SmartNotificationPolicyTest {
    private val now = 40 * ReviewReminderPolicy.DAY_MS
    private fun candidate(kind: SmartNoticeKind = SmartNoticeKind.ACCOUNT, key: String = "account:1") =
        SmartNoticeCandidate(key, "revision1", kind, now - 3 * 60 * 60_000L)
    private fun choose(candidates: List<SmartNoticeCandidate> = listOf(candidate()),
        states: Map<String, SmartNoticeState> = emptyMap(), last: Long = 0,
        foreground: Boolean = false, processing: Boolean = false, hour: Int = 11,
        permission: Boolean = true, snooze: Long = 0, ready: Boolean = true) =
        SmartNotificationPolicy.choose(candidates, states, now, hour, last, snooze,
            ready, permission, foreground, processing)

    @Test fun accountAndActivityShareOneBudgetAndAccountWins() {
        assertEquals(SmartNoticeKind.ACCOUNT, choose(listOf(candidate(SmartNoticeKind.ACTIVITY,"activity"),candidate()))?.kind)
        assertNull(choose(last = now - 23 * 60 * 60_000L))
        assertNotNull(choose(last = now - ReviewReminderPolicy.DAY_MS))
    }
    @Test fun resolvedWorkQuietHoursPermissionForegroundAndImportCannotPost() {
        assertNull(choose(emptyList()))
        assertNull(choose(hour = 20)); assertNull(choose(hour = 8))
        assertNull(choose(permission = false)); assertNull(choose(ready = false))
        assertNull(choose(foreground = true)); assertNull(choose(processing = true))
        assertNull(choose(snooze = now + 1))
        assertNull(choose(listOf(candidate().copy(evidenceAt = now - 60_000))))
    }
    @Test fun dismissalSuppressesSameEvidenceAndRevisionMayRequalify() {
        val dismissed = SmartNoticeState("revision1", dismissed = true)
        assertNull(choose(states = mapOf("account:1" to dismissed)))
        assertNotNull(choose(listOf(candidate().copy(revision = "revision2")), mapOf("account:1" to dismissed)))
    }
    @Test fun unchangedReviewWaitsSevenDaysButPaymentAndTipNeverRepeat() {
        assertNull(choose(states = mapOf("account:1" to SmartNoticeState("revision1", now - 2 * ReviewReminderPolicy.DAY_MS))))
        assertNotNull(choose(states = mapOf("account:1" to SmartNoticeState("revision1", now - 8 * ReviewReminderPolicy.DAY_MS))))
        for (kind in listOf(SmartNoticeKind.PAYMENT,SmartNoticeKind.DISCOVERY)) {
            assertNull(choose(listOf(candidate(kind)),mapOf("account:1" to SmartNoticeState("old",1))))
        }
    }
    @Test fun backwardClockFailsClosed() {
        assertNull(choose(last = now + 1))
        assertNull(choose(listOf(candidate().copy(evidenceAt = now + 1))))
    }
    @Test fun disabledCandidateCannotConsumeBudgetOrBlockAnotherType() {
        assertEquals("activity", choose(listOf(candidate().copy(enabled = false),
            candidate(SmartNoticeKind.ACTIVITY,"activity")))?.key)
    }
}

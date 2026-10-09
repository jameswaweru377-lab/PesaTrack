package com.kedevjimmy.pesatrack.domain.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageAccessPolicyTest {
    @Test fun noPermissionRequestsBeforeAcceptance() {
        assertEquals(MessageAccessAction.WAIT, MessageAccessPolicy.next(false, true, false, false))
    }
    @Test fun agreementAloneDoesNotLaunchPermissions() {
        assertEquals(MessageAccessAction.WAIT, MessageAccessPolicy.next(true, false, false, false))
    }
    @Test fun continueRequestsSmsFirst() {
        assertEquals(MessageAccessAction.REQUEST_SMS, MessageAccessPolicy.next(true, true, false, false))
    }
    @Test fun smsDecisionAdvancesToNotificationSettings() {
        assertEquals(MessageAccessAction.OPEN_NOTIFICATIONS, MessageAccessPolicy.next(true, true, true, false))
    }
    @Test fun returningWithoutAccessStillContinues() {
        assertEquals(MessageAccessAction.CONTINUE, MessageAccessPolicy.next(true, true, true, true))
    }
}

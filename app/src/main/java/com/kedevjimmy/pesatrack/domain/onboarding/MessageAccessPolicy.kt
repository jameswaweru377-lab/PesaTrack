package com.kedevjimmy.pesatrack.domain.onboarding

enum class MessageAccessAction { WAIT, REQUEST_SMS, OPEN_NOTIFICATIONS, CONTINUE }

object MessageAccessPolicy {
    fun next(accepted: Boolean, started: Boolean, smsHandled: Boolean,
        notificationsHandled: Boolean): MessageAccessAction = when {
        !accepted || !started -> MessageAccessAction.WAIT
        !smsHandled -> MessageAccessAction.REQUEST_SMS
        !notificationsHandled -> MessageAccessAction.OPEN_NOTIFICATIONS
        else -> MessageAccessAction.CONTINUE
    }
}

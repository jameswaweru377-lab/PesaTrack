package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import java.util.Calendar
import org.junit.Test

class BillNotificationCopyTest {
    @Test fun rentUsesItsSavedCategoryNotTheMerchantsName() {
        assertEquals("RENT", BillNotificationCopy.badge(TransactionCategory.RENT))
        assertEquals("Rent due soon", BillNotificationCopy.headline(TransactionCategory.RENT))
        assertEquals("Your scheduled rent is coming up.", BillNotificationCopy.description(TransactionCategory.RENT))
        assertEquals("BILL", BillNotificationCopy.badge(TransactionCategory.OTHER))
        assertEquals("Your scheduled bill is coming up.", BillNotificationCopy.description(TransactionCategory.OTHER))
    }

    @Test fun dueLabelUsesCalendarDaysRatherThanHoursUntilDue() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 13, 23, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tomorrow = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 8)
        }
        assertEquals("Due tomorrow", BillNotificationCopy.dueLabel(now.timeInMillis, tomorrow.timeInMillis))
        tomorrow.add(Calendar.DAY_OF_MONTH, 2)
        assertEquals("Due in 3 days", BillNotificationCopy.dueLabel(now.timeInMillis, tomorrow.timeInMillis))
    }

    @Test fun merchantIsReadableAndDoesNotExposeTheAccountSuffix() {
        assertEquals("Harbor Property Services", BillNotificationCopy.merchant(null, "HARBOR PROPERTY SERVICES for account 1234"))
        assertEquals("Harbor Properties", BillNotificationCopy.merchant("Harbor Properties", "other saved name"))
        assertEquals("Scheduled bill", BillNotificationCopy.merchant(null, "for account 1234"))
    }
}

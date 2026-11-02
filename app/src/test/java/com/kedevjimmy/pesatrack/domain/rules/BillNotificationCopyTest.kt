package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class BillNotificationCopyTest {
    @Test fun rentUsesItsSavedCategoryNotTheMerchantsName() {
        assertEquals("RENT", BillNotificationCopy.badge(TransactionCategory.RENT))
        assertEquals("Your scheduled rent is coming up.", BillNotificationCopy.description(TransactionCategory.RENT))
        assertEquals("BILL", BillNotificationCopy.badge(TransactionCategory.OTHER))
        assertEquals("Your scheduled bill is coming up.", BillNotificationCopy.description(TransactionCategory.OTHER))
    }

    @Test fun merchantIsReadableAndDoesNotExposeTheAccountSuffix() {
        assertEquals("Harbor Property Services", BillNotificationCopy.merchant(null, "HARBOR PROPERTY SERVICES for account 1234"))
        assertEquals("Harbor Properties", BillNotificationCopy.merchant("Harbor Properties", "other saved name"))
        assertEquals("Scheduled bill", BillNotificationCopy.merchant(null, "for account 1234"))
    }
}

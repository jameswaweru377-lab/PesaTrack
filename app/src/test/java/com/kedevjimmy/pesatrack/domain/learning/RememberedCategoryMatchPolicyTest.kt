package com.kedevjimmy.pesatrack.domain.learning

import org.junit.Assert.*
import org.junit.Test

class RememberedCategoryMatchPolicyTest {
    @Test fun exactNamesOnly() {
        assertTrue(RememberedCategoryMatchPolicy.matches("ZD", null, " zd ", null))
        assertFalse(RememberedCategoryMatchPolicy.matches("ZD", null, "ZD OTHER", null))
        assertFalse(RememberedCategoryMatchPolicy.matches("James", null, "James Mwengo", null))
    }
    @Test fun phonesTakePrecedenceAndMissingPhoneDoesNotBroadenRule() {
        assertTrue(RememberedCategoryMatchPolicy.matches("James", "0712345678", "JAMES", "+254712345678"))
        assertFalse(RememberedCategoryMatchPolicy.matches("James", "0712345678", "James", "0798765432"))
        assertFalse(RememberedCategoryMatchPolicy.matches("James", "0712345678", "James", null))
    }
}

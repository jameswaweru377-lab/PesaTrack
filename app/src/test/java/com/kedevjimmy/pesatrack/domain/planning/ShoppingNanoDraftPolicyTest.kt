package com.kedevjimmy.pesatrack.domain.planning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShoppingNanoDraftPolicyTest {
    @Test fun acceptsGroundedProductLinesButNotReceiptFooter() {
        val original = "Mango 80\nApples 2kg 500\nThank you for shopping"
        val output = "1 x Mango @ 80\n1 x Apples 2kg @ 500\nThank you for shopping"
        assertEquals("1 x Mango @ 80\n1 x Apples 2kg @ 500",
            ShoppingNanoDraftPolicy.reviewableDraft(original, output))
    }

    @Test fun leavesAmbiguousTotalWithoutInventingUnitPrice() {
        val original = "20 tomatoes 60"
        assertEquals("20 x tomatoes",
            ShoppingNanoDraftPolicy.reviewableDraft(original, "20 x tomatoes"))
    }

    @Test fun rejectsInventedProductOrInventedAmount() {
        assertNull(ShoppingNanoDraftPolicy.reviewableDraft("Bread 80", "1 x Milk @ 80"))
        assertNull(ShoppingNanoDraftPolicy.reviewableDraft("Bread 80", "1 x Bread @ 950"))
    }

    @Test fun rejectsUnstructuredOrEmptyModelOutput() {
        assertNull(ShoppingNanoDraftPolicy.reviewableDraft("Bread 80", "Here is your shopping list:"))
        assertNull(ShoppingNanoDraftPolicy.reviewableDraft("Bread 80", ""))
        assertNull(ShoppingNanoDraftPolicy.reviewableDraft("", "1 x Bread @ 80"))
    }

    @Test fun promptForbidsGuessingAndExplicitlyRequiresReviewableFormat() {
        val prompt = ShoppingNanoDraftPolicy.prompt("20 tomatoes 60")
        assertEquals(true, prompt.contains("20 tomatoes 60"))
        assertEquals(true, prompt.contains("Do not guess"))
        assertEquals(true, prompt.contains("@"))
    }
}

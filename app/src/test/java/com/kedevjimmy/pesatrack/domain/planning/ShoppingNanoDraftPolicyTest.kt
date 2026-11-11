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

    @Test fun doesNotPromoteAmbiguousMultiItemTotalToUnitPrice() {
        assertEquals("20 x tomatoes",
            ShoppingNanoDraftPolicy.reviewableDraft("20 tomatoes 60", "20 x tomatoes @ 60"))
        assertEquals("20 x tomatoes @ 60",
            ShoppingNanoDraftPolicy.reviewableDraft("20 tomatoes @ 60", "20 x tomatoes @ 60"))
    }

    @Test fun cannotBorrowPriceFromAnotherProductOrReceiptTotal() {
        val receipt = "Bread 80\nMilk 250\nTotal 330"
        assertEquals("1 x Bread",
            ShoppingNanoDraftPolicy.reviewableDraft(receipt, "1 x Bread @ 250"))
        assertEquals("1 x Bread",
            ShoppingNanoDraftPolicy.reviewableDraft(receipt, "1 x Bread @ 330"))
    }

    @Test fun cannotInventSingleItemWhenSourceExplicitlyHasTwenty() {
        assertNull(ShoppingNanoDraftPolicy.reviewableDraft("20 tomatoes 60", "1 x tomatoes @ 60"))
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

    @Test fun rejectsReceiptMetadataEvenIfModelFormatsItAsAnItem() {
        val original = "O and D Perfumes Your Perfumes\nSale receipt no 33\n" +
            "Sold by Joyce\nCustomer James\nAfnan 9 PM Rebel one piece 5700\n" +
            "Discount 570\nAmount to pay 5170\nLoyalty points balance 10\n" +
            "Thank you for your purchase\nPlease note that returns require a receipt"
        val model = "1 x O and D Perfumes\n1 x Sold by Joyce\n" +
            "1 x Afnan 9 PM Rebel @ 5700\n1 x Discount @ 570\n" +
            "1 x Loyalty points balance 10\n1 x Thank you for your purchase\n" +
            "1 x Please note that returns require a receipt"
        assertEquals("1 x Afnan 9 PM Rebel @ 5700",
            ShoppingNanoDraftPolicy.reviewableDraft(original, model))
    }

    @Test fun promptForbidsGuessingAndExplicitlyRequiresReviewableFormat() {
        val prompt = ShoppingNanoDraftPolicy.prompt("20 tomatoes 60")
        assertEquals(true, prompt.contains("20 tomatoes 60"))
        assertEquals(true, prompt.contains("Do not guess"))
        assertEquals(true, prompt.contains("@"))
    }
}

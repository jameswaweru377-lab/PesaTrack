package com.kedevjimmy.pesatrack.domain.planning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingIntelligencePolicyTest {
    private fun shoppingList(
        id: String,
        month: String,
        created: Long,
        vararg items: ShoppingItem
    ) = ShoppingList(id, "Monthly shopping", month, created, items.toList())

    @Test fun scansSimpleItemsWithExplicitUnitPriceAndQuantity() {
        val lines = ShoppingIntelligencePolicy.parseLines(
            "• Sugar x2 @ KSh 210.50\n2 x Milk KES 120\nBread\nRice 2kg"
        )
        assertEquals(listOf("Sugar", "Milk", "Bread", "Rice 2kg"), lines.map { it.name })
        assertEquals(listOf(2, 2, 1, 1), lines.map { it.quantity })
        assertEquals(listOf(21050L, 12000L, null, null), lines.map { it.unitPriceMinor })
    }

    @Test fun excludesReceiptTotalsAndAmbiguousPrices() {
        val lines = ShoppingIntelligencePolicy.parseLines(
            "TOTAL 500\nSubtotal 400\nVAT 50\nMilk 250\nEggs @ KSh 1000001\nDate: 03/11/2026"
        )
        assertEquals(listOf("Milk 250", "Eggs"), lines.map { it.name })
        assertTrue(lines.all { it.unitPriceMinor == null })
    }

    @Test fun receiptScanKeepsPurchasedItemsButNotMerchantOrFooterText() {
        val receipt = """
            O and D Perfumes Your Perfumes
            Sale receipt no 33
            Sold by Joyce
            Customer James
            Afnan 9 PM Rebel @ KSh 5700
            Subtotal 5700
            Discount 570
            Total 5170
            Amount given M-Pesa 5170
            Loyalty points gained 10
            Thank you for your purchase
            Please keep your receipt for returns
            Website www.example.com
        """.trimIndent()
        val items = ShoppingIntelligencePolicy.parseLines(receipt)
        assertEquals(listOf("Afnan 9 PM Rebel"), items.map { it.name })
        assertEquals(listOf(570000L), items.map { it.unitPriceMinor })
    }

    @Test fun noTextOrOnlyTotalsDoesNotCreateItems() {
        assertTrue(ShoppingIntelligencePolicy.parseLines(" ").isEmpty())
        assertTrue(ShoppingIntelligencePolicy.parseLines("TOTAL 300\nChange 20").isEmpty())
    }

    @Test fun importSkipsExistingAndScannedDuplicatesWithoutChangingExisting() {
        val list = shoppingList("one", "2026-11", 1L,
            ShoppingItem("one", "Milk", 1, 10000L, bought = true))
        var n = 0
        val result = ShoppingIntelligencePolicy.importCandidates(list, listOf(
            ShoppingScanCandidate("milk", 2, 12000L),
            ShoppingScanCandidate("Rice", 1, null),
            ShoppingScanCandidate(" RICE ", 1, 10000L),
            ShoppingScanCandidate("Sugar", 3, 5000L)
        )) { "new-${++n}" }
        assertEquals(2, result.addedCount)
        assertEquals(2, result.skippedDuplicateCount)
        assertEquals(0, result.skippedCapacityCount)
        assertEquals(listOf("Milk", "Rice", "Sugar"), result.updated.items.map { it.name })
        assertTrue(result.updated.items.first().bought)
        assertEquals(1, list.items.size)
    }

    @Test fun importRespectsHundredItemLimit() {
        val list = shoppingList("full", "2026-11", 1L, *Array(99) { i ->
            ShoppingItem("existing-$i", "Item $i", 1, null)
        })
        val result = ShoppingIntelligencePolicy.importCandidates(list, listOf(
            ShoppingScanCandidate("Rice", 1, 10000L), ShoppingScanCandidate("Tea", 1, null)
        )) { "last" }
        assertEquals(100, result.updated.items.size)
        assertEquals(1, result.addedCount)
        assertEquals(1, result.skippedCapacityCount)
    }

    @Test fun suggestionsCountDistinctMonthsExcludeCurrentNamesAndKeepPricesAsHints() {
        val selected = shoppingList("now", "2026-11", 300L,
            ShoppingItem("n", "Rice", 1, null))
        val history = listOf(
            shoppingList("oct", "2026-10", 200L,
                ShoppingItem("a", "Milk", 2, 12000L), ShoppingItem("b", "Rice", 1, 50000L)),
            shoppingList("oct2", "2026-10", 201L,
                ShoppingItem("c", "milk", 1, 13000L)),
            shoppingList("sep", "2026-09", 100L,
                ShoppingItem("d", "Milk", 1, null), ShoppingItem("e", "Tea", 1, 9000L))
        )
        val suggestions = ShoppingIntelligencePolicy.suggest(history + selected, selected)
        assertEquals(listOf("milk", "Tea"), suggestions.map { it.name })
        assertEquals(2, suggestions.first().monthsSeen)
        assertEquals(13000L, suggestions.first().previousUnitPriceMinor)
        assertFalse(suggestions.any { it.name.equals("Rice", ignoreCase = true) })
        // A suggestion is not a verified price for the new month.
        assertNull(ShoppingIntelligencePolicy.proposedSuggestionItem(suggestions.first(), "suggested").unitPriceMinor)
    }

    @Test fun historyGroupsByMonthAndKeepsPartialEstimatesDistinctFromSpending() {
        val history = ShoppingIntelligencePolicy.history(listOf(
            shoppingList("oct-a", "2026-10", 100L,
                ShoppingItem("a", "Milk", 2, 12000L, bought = true),
                ShoppingItem("b", "Bread", 1, null)),
            shoppingList("oct-b", "2026-10", 110L,
                ShoppingItem("c", "Tea", 1, 4500L)),
            shoppingList("sep", "2026-09", 50L, ShoppingItem("d", "Rice", 1, 30000L))
        ))
        assertEquals(listOf("2026-10", "2026-09"), history.map { it.month })
        assertEquals(2, history.first().listCount)
        assertEquals(3, history.first().itemCount)
        assertEquals(1, history.first().checkedCount)
        assertEquals(28500L, history.first().knownEstimateMinor)
        assertEquals(1, history.first().missingPriceCount)
    }
}

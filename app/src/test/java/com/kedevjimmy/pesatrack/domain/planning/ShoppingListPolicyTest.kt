package com.kedevjimmy.pesatrack.domain.planning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingListPolicyTest {
    private fun base() = ShoppingList(
        id = "list-a", title = "Monthly shopping", month = "2026-11",
        createdAtEpochMs = 1000L,
        items = listOf(
            ShoppingItem("rice", "Rice", 2, 42000L, bought = true),
            ShoppingItem("sugar", "Sugar", 1, null)
        )
    )

    @Test fun estimatesOnlyKnownPricesAndFlagsMissing() {
        val estimate = ShoppingListPolicy.total(base())
        assertEquals(84000L, estimate.knownTotalMinor)
        assertEquals(1, estimate.missingPrices)
    }

    @Test fun priceParsingUsesExactMinorUnits() {
        assertEquals(25050L, ShoppingListPolicy.parsePriceMinor("250.50"))
        assertEquals(120000L, ShoppingListPolicy.parsePriceMinor("1200"))
        assertNull(ShoppingListPolicy.parsePriceMinor("  "))
        assertThrows(IllegalArgumentException::class.java) {
            ShoppingListPolicy.parsePriceMinor("1.234")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ShoppingListPolicy.parsePriceMinor("-20")
        }
    }

    @Test fun rejectsInvalidInputBeforeSaving() {
        assertThrows(IllegalArgumentException::class.java) {
            ShoppingListPolicy.validate(base().copy(title = " "))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ShoppingListPolicy.validate(base().copy(month = "2026-13"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ShoppingListPolicy.validate(base().copy(items = listOf(
                ShoppingItem("a", "Milk", 0, 20000L)
            )))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ShoppingListPolicy.validate(base().copy(items = listOf(
                ShoppingItem("a", "Rice", 2, -1L)
            )))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ShoppingListPolicy.validate(base().copy(items = listOf(
                ShoppingItem("a", "Milk", 1, null),
                ShoppingItem("a", "Tea", 1, null)
            )))
        }
    }

    @Test fun reuseIsANewUnboughtListAndLeavesOriginalAlone() {
        val original = base()
        val next = ShoppingListPolicy.reuse(original, "list-b", "2026-12", 2000L)
        assertEquals("list-b", next.id)
        assertEquals("2026-12", next.month)
        assertFalse(next.items.first().bought)
        assertTrue(original.items.first().bought)
        assertEquals(original.items.map { it.name }, next.items.map { it.name })
    }

    @Test fun tooManyItemsAreRejected() {
        val item = ShoppingItem("x", "Item", 1, 1L)
        assertThrows(IllegalArgumentException::class.java) {
            ShoppingListPolicy.validate(base().copy(items = List(101) { index ->
                item.copy(id = "item-$index")
            }))
        }
    }
}

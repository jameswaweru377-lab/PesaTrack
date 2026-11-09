package com.kedevjimmy.pesatrack.domain.planning

/** Planned purchases are not transactions, spending, bills, or account balances. */
data class ShoppingItem(
    val id: String,
    val name: String,
    val quantity: Int,
    val unitPriceMinor: Long?,
    val bought: Boolean = false
)

data class ShoppingList(
    val id: String,
    val title: String,
    val month: String,
    val createdAtEpochMs: Long,
    val items: List<ShoppingItem> = emptyList()
)

data class ShoppingEstimate(val knownTotalMinor: Long, val missingPrices: Int)

object ShoppingListPolicy {
    const val MAX_LISTS = 36
    const val MAX_ITEMS = 100
    private const val MAX_UNIT_PRICE_MINOR = 100_000_000L // KSh 1,000,000
    private val monthPattern = Regex("20[0-9]{2}-(0[1-9]|1[0-2])")
    private val pricePattern = Regex("[0-9]{1,7}(\\.[0-9]{1,2})?")

    fun validate(list: ShoppingList): ShoppingList {
        require(list.id.length in 1..80 && list.id.isNotBlank()) { "Invalid list ID." }
        require(list.title.trim().length in 1..60) { "Enter a list name (up to 60 characters)." }
        require(monthPattern.matches(list.month)) { "Choose a valid month." }
        require(list.createdAtEpochMs > 0) { "Invalid list date." }
        require(list.items.size <= MAX_ITEMS) { "A list can have up to 100 items." }
        require(list.items.map { it.id }.distinct().size == list.items.size) {
            "Duplicate shopping item."
        }
        list.items.forEach { item ->
            require(item.id.length in 1..80 && item.id.isNotBlank()) { "Invalid item ID." }
            require(item.name.trim().length in 1..80) { "Enter an item name (up to 80 characters)." }
            require(item.quantity in 1..999) { "Quantity must be between 1 and 999." }
            require(item.unitPriceMinor == null || item.unitPriceMinor in 0..MAX_UNIT_PRICE_MINOR) {
                "Price must be between KSh 0 and KSh 1,000,000."
            }
        }
        return list
    }

    /** Empty means unknown; the user must not mistake it for a free purchase. */
    fun parsePriceMinor(raw: String): Long? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        require(pricePattern.matches(text)) { "Enter a valid price, e.g. 250.50." }
        val parts = text.split('.')
        val minor = parts[0].toLong() * 100L +
            (parts.getOrNull(1)?.padEnd(2, '0')?.toLong() ?: 0L)
        require(minor <= MAX_UNIT_PRICE_MINOR) { "Price is too high for a shopping item." }
        return minor
    }

    fun total(list: ShoppingList): ShoppingEstimate {
        validate(list)
        return ShoppingEstimate(
            knownTotalMinor = list.items.sumOf { item ->
                (item.unitPriceMinor ?: 0L) * item.quantity
            },
            missingPrices = list.items.count { it.unitPriceMinor == null }
        )
    }

    fun reuse(source: ShoppingList, newId: String, month: String, createdAtEpochMs: Long): ShoppingList =
        validate(source.copy(
            id = newId,
            month = month,
            createdAtEpochMs = createdAtEpochMs,
            items = source.items.map { it.copy(bought = false) }
        ))
}

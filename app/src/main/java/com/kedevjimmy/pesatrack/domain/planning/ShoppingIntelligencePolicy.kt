package com.kedevjimmy.pesatrack.domain.planning

import java.util.Locale

/** Unverified text from a chosen image; only an explicit user confirmation may save it. */
data class ShoppingScanCandidate(
    val name: String,
    val quantity: Int,
    val unitPriceMinor: Long?
)

data class ShoppingImportResult(
    val updated: ShoppingList,
    val addedCount: Int,
    val skippedDuplicateCount: Int,
    val skippedCapacityCount: Int
)

data class ShoppingSuggestion(
    val name: String,
    val monthsSeen: Int,
    val previousUnitPriceMinor: Long?
)

data class ShoppingMonthSummary(
    val month: String,
    val listCount: Int,
    val itemCount: Int,
    val checkedCount: Int,
    val knownEstimateMinor: Long,
    val missingPriceCount: Int
)

/** No ledger queries, OCR library, Android classes, or network in this policy. */
object ShoppingIntelligencePolicy {
    private val priceTag = Regex(
        "(?i)\\s*(?:@\\s*)?(?:KSh|KES|Sh)\\.?\\s*([0-9][0-9.,]*)\\s*$|\\s*@\\s*([0-9][0-9.,]*)\\s*$"
    )
    private val leadingQuantity = Regex("^([0-9]{1,3})\\s*[x×]\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val trailingQuantity = Regex("^(.+?)\\s+[x×]\\s*([0-9]{1,3})$", RegexOption.IGNORE_CASE)
    private val lineNumber = Regex("^[0-9]{1,3}[.)]\\s+")
    private val bullet = Regex("^[•*\\-]\\s*")
    private val noise = Regex(
        "(?i)^(?:sub\\s*total|total|vat|tax|change|date|receipt|balance|payment|cash|" +
            "thank\\s*you|shopping\\s*list|items?\\s*:)\\b.*"
    )

    private fun normalized(name: String): String = name.trim()
        .replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

    /** Only explicit KSh/KES/@ prices are parsed; plain trailing numbers stay as text. */
    fun parseLines(text: String): List<ShoppingScanCandidate> = text.take(24_000)
        .lineSequence().take(200).mapNotNull(::parseLine).toList()

    private fun parseLine(line: String): ShoppingScanCandidate? {
        var value = line.trim().replace(lineNumber, "").replace(bullet, "").trim()
        if (value.isEmpty() || noise.matches(value) || value.all { it.isDigit() || it in ",. /-" }) {
            return null
        }
        val priceMatch = priceTag.find(value)
        val proposedPrice = if (priceMatch != null) {
            value = value.substring(0, priceMatch.range.first).trimEnd()
            val raw = priceMatch.groupValues.drop(1).firstOrNull { it.isNotEmpty() }.orEmpty()
            try { ShoppingListPolicy.parsePriceMinor(raw) }
            catch (_: IllegalArgumentException) { null }
        } else null
        val prefix = leadingQuantity.matchEntire(value)
        val suffix = if (prefix == null) trailingQuantity.matchEntire(value) else null
        val quantity = (prefix?.groupValues?.get(1) ?: suffix?.groupValues?.get(2))?.toIntOrNull() ?: 1
        val name = (prefix?.groupValues?.get(2) ?: suffix?.groupValues?.get(1) ?: value)
            .trim().replace(Regex("\\s+"), " ").trimEnd('-', ':').trim()
        if (quantity !in 1..999 || name.length !in 1..80 || name.none(Char::isLetter) ||
            noise.matches(name)) return null
        return ShoppingScanCandidate(name, quantity, proposedPrice)
    }

    /** Never overwrite a saved item or exceed the existing 100-item limit. */
    fun importCandidates(
        existing: ShoppingList,
        candidates: List<ShoppingScanCandidate>,
        ids: () -> String
    ): ShoppingImportResult {
        ShoppingListPolicy.validate(existing)
        val seen = existing.items.mapTo(mutableSetOf()) { normalized(it.name) }
        val additions = mutableListOf<ShoppingItem>()
        var duplicate = 0
        var full = 0
        candidates.forEach { candidate ->
            val cleanName = candidate.name.trim().replace(Regex("\\s+"), " ")
            if (cleanName.isEmpty() || cleanName.length > 80 || candidate.quantity !in 1..999 ||
                candidate.unitPriceMinor != null && candidate.unitPriceMinor !in 0..100_000_000L) {
                return@forEach
            }
            if (!seen.add(normalized(cleanName))) duplicate++
            else if (existing.items.size + additions.size >= ShoppingListPolicy.MAX_ITEMS) full++
            else additions += ShoppingItem(ids(), cleanName, candidate.quantity,
                candidate.unitPriceMinor, bought = false)
        }
        val updated = ShoppingListPolicy.validate(existing.copy(items = existing.items + additions))
        return ShoppingImportResult(updated, additions.size, duplicate, full)
    }

    /** Ranked by months independently, not the number of copies within a month. */
    fun suggest(lists: List<ShoppingList>, selected: ShoppingList): List<ShoppingSuggestion> {
        val present = selected.items.mapTo(mutableSetOf()) { normalized(it.name) }
        val prior = lists.filter { it.id != selected.id && it.createdAtEpochMs <= selected.createdAtEpochMs }
        data class Occurrence(val month: String, val created: Long, val item: ShoppingItem)
        return prior.flatMap { list -> list.items.map { Occurrence(list.month, list.createdAtEpochMs, it) } }
            .groupBy { normalized(it.item.name) }
            .filterKeys { it.isNotBlank() && it !in present }
            .map { (_, occurrences) ->
                val recent = occurrences.sortedByDescending { it.created }
                Triple(recent.first().created, occurrences.map { it.month }.distinct().size,
                    ShoppingSuggestion(recent.first().item.name,
                        occurrences.map { it.month }.distinct().size,
                        recent.firstOrNull { it.item.unitPriceMinor != null }?.item?.unitPriceMinor))
            }
            .sortedWith(compareByDescending<Triple<Long, Int, ShoppingSuggestion>> { it.second }
                .thenByDescending { it.first }.thenBy { it.third.name })
            .take(8).map { it.third }
    }

    /** Previous estimate is information, not automatically trusted as today's price. */
    fun proposedSuggestionItem(suggestion: ShoppingSuggestion, id: String): ShoppingItem =
        ShoppingItem(id, suggestion.name, 1, null, bought = false)

    fun history(lists: List<ShoppingList>): List<ShoppingMonthSummary> = lists
        .groupBy { it.month }
        .toSortedMap(reverseOrder())
        .map { (month, monthLists) ->
            val estimates = monthLists.map(ShoppingListPolicy::total)
            ShoppingMonthSummary(
                month = month,
                listCount = monthLists.size,
                itemCount = monthLists.sumOf { it.items.size },
                checkedCount = monthLists.sumOf { it.items.count(ShoppingItem::bought) },
                knownEstimateMinor = estimates.sumOf { it.knownTotalMinor },
                missingPriceCount = estimates.sumOf { it.missingPrices }
            )
        }
}

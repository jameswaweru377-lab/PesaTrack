package com.kedevjimmy.pesatrack.domain.planning

import java.util.Locale

/** A strictly bounded, user-reviewable interpretation; Nano never owns financial facts. */
object ShoppingNanoDraftPolicy {
    private const val MAX_SOURCE = 6_000
    private const val MAX_RESPONSE = 6_000
    private const val MAX_LINES = 100
    private val itemFormat = Regex(
        "^([1-9][0-9]{0,2})\\s+x\\s+(.{1,80}?)(?:\\s+@\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?))?$",
        RegexOption.IGNORE_CASE
    )
    private val words = Regex("[\\p{L}\\p{N}]+")
    private val receiptMetadata = Regex(
        "^(?:thank\\s+you|please\\s+note|sold\\s+by|customer(?:\\s+name)?|" +
            "sale\\s+receipt|receipt(?:\\s+(?:no|number))?|bill\\s+(?:no|number)|" +
            "loyalty\\s+points?|amount\\s+(?:to\\s+pay|given|paid)|" +
            "sub\\s*total|grand\\s+total|total|discount|vat|tax|change|" +
            "account\\s+(?:no|number)|pin|telephone|phone|website|www\\.|" +
            "payment|mpesa|m-pesa|your\\s+perfumes)\\b.*",
        RegexOption.IGNORE_CASE
    )

    fun prompt(ocrText: String): String = """
        You are helping a private shopping planner interpret OCR text from a receipt or handwritten list.
        Extract only real products that are explicitly present in the source text below.
        Do not guess products, amounts, quantities, discounts or arithmetic.
        Ignore shop names, receipts headings, dates, payment details, totals and thank-you notes.
        Return only item lines in this exact format, without bullets, explanations or Markdown:
        1 x Mango @ 80
        20 x Tomatoes
        1 x Apples 2kg @ 500
        The number after @ is the UNIT price in Kenyan shillings, not the receipt total.
        If you cannot prove a number is the unit price, omit the @ and price entirely.
        Use 1 x for items without an explicit item count. Preserve package sizes inside the name.
        If no products can be identified, return an empty response.
        Source OCR text:
        ${ocrText.take(MAX_SOURCE)}
    """.trimIndent()

    /** Reject invented items/prices before offering Nano lines for explicit human review. */
    fun reviewableDraft(ocrText: String, modelText: String): String? {
        if (ocrText.isBlank() || modelText.isBlank() || modelText.length > MAX_RESPONSE) return null
        val source = ocrText.take(MAX_SOURCE).lowercase(Locale.ROOT).replace(",", "")
        val sourceWords = words.findAll(source).map { it.value }.toSet()
        val sourceLines = source.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val isReceipt = sourceLines.take(5).any {
            it.contains("receipt") || it.contains("invoice") || it.contains("sold by")
        }
        val firstLine = sourceLines.firstOrNull().orEmpty()
        val verified = modelText.lineSequence().take(MAX_LINES).mapNotNull { untrusted ->
            val line = untrusted.trim()
            val match = itemFormat.matchEntire(line) ?: return@mapNotNull null
            val candidate = ShoppingIntelligencePolicy.parseLines(line).singleOrNull()
                ?: return@mapNotNull null
            if (receiptMetadata.matches(candidate.name.trim())) return@mapNotNull null
            // A business name at the top of a sale receipt is not a purchased item.
            if (isReceipt && firstLine.startsWith(candidate.name.lowercase(Locale.ROOT))) {
                return@mapNotNull null
            }
            if (words.findAll(candidate.name.lowercase(Locale.ROOT))
                    .map { it.value }.any { it !in sourceWords }) return@mapNotNull null
            val quantity = match.groupValues[1]
            if (quantity != "1" && !Regex("(?<!\\d)${Regex.escape(quantity)}(?!\\d)")
                    .containsMatchIn(source)) return@mapNotNull null
            val price = match.groupValues[3].replace(",", "")
            if (price.isNotEmpty() && !Regex("(?<![\\d.])${Regex.escape(price)}(?![\\d.])")
                    .containsMatchIn(source)) return@mapNotNull null
            line
        }.toList()
        return verified.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }
}

package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** Groups amounts for display only; the stored edit value and money parser stay unchanged. */
internal fun groupedMoneyInput(raw: String): String {
    if (raw.isEmpty()) return raw
    val decimalIndex = raw.indexOf('.')
    val integral = (if (decimalIndex < 0) raw else raw.substring(0, decimalIndex))
        .replace(",", "")
    val sign = if (integral.startsWith('-')) "-" else ""
    val digits = integral.removePrefix("-")
    val groups = digits.reversed().chunked(3).joinToString(",").reversed()
    return sign + groups + if (decimalIndex < 0) "" else raw.substring(decimalIndex)
}

/** Keeps the cursor attached to its source digit when grouping separators appear. */
internal object MoneyGroupingVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val displayed = groupedMoneyInput(raw)
        val originalPositions = IntArray(raw.length + 1)
        val transformedPositions = IntArray(displayed.length + 1)
        for (sourceOffset in 0..raw.length) {
            val sourceCharacters = raw.take(sourceOffset).count { it != ',' }
            var consumed = 0
            var targetOffset = 0
            while (targetOffset < displayed.length && consumed < sourceCharacters) {
                if (displayed[targetOffset] != ',') consumed++
                targetOffset++
            }
            while (targetOffset < displayed.length && displayed[targetOffset] == ',') targetOffset++
            originalPositions[sourceOffset] = targetOffset
        }
        for (targetOffset in 0..displayed.length) {
            val targetCharacters = displayed.take(targetOffset).count { it != ',' }
            var consumed = 0
            var sourceOffset = 0
            while (sourceOffset < raw.length && consumed < targetCharacters) {
                if (raw[sourceOffset] != ',') consumed++
                sourceOffset++
            }
            transformedPositions[targetOffset] = sourceOffset
        }
        return TransformedText(AnnotatedString(displayed), object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                originalPositions[offset.coerceIn(0, raw.length)]

            override fun transformedToOriginal(offset: Int): Int =
                transformedPositions[offset.coerceIn(0, displayed.length)]
        })
    }
}

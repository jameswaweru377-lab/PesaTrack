package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyInputGroupingTest {
    @Test fun `groups thousands without changing value`() {
        assertEquals("500", groupedMoneyInput("500"))
        assertEquals("1,000", groupedMoneyInput("1000"))
        assertEquals("50,000", groupedMoneyInput("50000"))
        assertEquals("1,250,000.75", groupedMoneyInput("1250000.75"))
    }

    @Test fun `preserves decimal editing and signs`() {
        assertEquals("12,500.", groupedMoneyInput("12500."))
        assertEquals("-1,500.50", groupedMoneyInput("-1500.50"))
        assertEquals("50,000", groupedMoneyInput("50,000"))
        assertEquals("", groupedMoneyInput(""))
    }

    @Test fun `keeps editing offsets valid around inserted commas`() {
        val transformed = MoneyGroupingVisualTransformation.filter(AnnotatedString("50000.25"))
        assertEquals("50,000.25", transformed.text.text)
        assertEquals(9, transformed.offsetMapping.originalToTransformed(8))
        assertEquals(8, transformed.offsetMapping.transformedToOriginal(9))
        assertEquals(2, transformed.offsetMapping.transformedToOriginal(3))
    }
}

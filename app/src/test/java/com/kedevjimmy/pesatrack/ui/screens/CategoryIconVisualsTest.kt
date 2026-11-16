package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.ui.graphics.Color
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryIconVisualsTest {
    @Test fun approvedCategoriesHaveDistinctIconsAndTints() {
        val labels = listOf(
            "Internet", "Phone & data", "Home & household", "Shopping", "Health",
            "Education", "Entertainment", "Personal care", "Family support",
            "Fees", "Debt repayment", "Savings", "Investments", "Income"
        )
        val visuals = labels.map(::categoryIconVisual)
        assertEquals(labels.size, visuals.map { it.icon.name }.distinct().size)
        assertEquals(labels.size, visuals.map { it.tint }.distinct().size)
        assertNotEquals(categoryIconVisual("Internet").icon, categoryIconVisual("Phone & data").icon)
    }

    @Test fun activityUsesTheSameCategoryVisuals() {
        for (label in listOf("Electricity", "Fees", "Personal care", "Rent", "Water")) {
            assertEquals(
                categoryIconVisual(label),
                activityCategoryIconVisual(label, MoneyDirection.EXPENSE)
            )
        }
    }

    @Test fun transferAndAllocationIconsStayMeaningful() {
        assertEquals(
            categoryIconVisual("Transfer"),
            activityCategoryIconVisual("Other", MoneyDirection.TRANSFER)
        )
        for (label in listOf("Savings", "Investments", "Debt repayment")) {
            assertEquals(
                categoryIconVisual(label),
                activityCategoryIconVisual(label, MoneyDirection.TRANSFER)
            )
        }
        assertEquals(
            categoryIconVisual("Income"),
            activityCategoryIconVisual("Uncategorized", MoneyDirection.INCOME)
        )
    }

    @Test fun categoryIconsMeetTextContrastOnRealPickerAndActivityTiles() {
        // Both screens render small pictograms on these two actual surfaces.
        val tiles = listOf(BrownHomePalette.cream, BrownHomePalette.white)
        val labels = listOf(
            "Rent", "Electricity", "Water", "Internet", "Phone & data",
            "Home & household", "Groceries", "Shopping", "Eating out", "Health",
            "Education", "Entertainment", "Personal care", "Family support",
            "Fees", "Debt repayment", "Other", "Savings", "Investments",
            "Income", "Transport", "Transfer", "Uncategorized"
        )
        for (label in labels) {
            val icon = categoryIconVisual(label)
            for (background in tiles) {
                assertTrue(
                    "$label icon contrast is ${contrastRatio(icon.tint, background)} on $background",
                    contrastRatio(icon.tint, background) >= 4.5
                )
            }
        }
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val light = max(relativeLuminance(a), relativeLuminance(b))
        val dark = min(relativeLuminance(a), relativeLuminance(b))
        return (light + 0.05) / (dark + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linear(channel: Float): Double {
            val c = channel.toDouble()
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(color.red) +
            0.7152 * linear(color.green) +
            0.0722 * linear(color.blue)
    }
}

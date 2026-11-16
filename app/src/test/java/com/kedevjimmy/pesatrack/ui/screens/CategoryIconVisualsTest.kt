package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
        for (label in listOf("Savings", "Investments")) {
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
}

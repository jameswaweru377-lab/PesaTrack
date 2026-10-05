package com.kedevjimmy.pesatrack.ui

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.model.*
import com.kedevjimmy.pesatrack.ui.screens.*
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class SalaryCompactUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db")
    }
    @After fun clean() { compose.runOnIdle { }; PesaTrackDatabase.resetForTests(); context.deleteDatabase("pesatrack.db") }
    private fun at(date: String) = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)!!.time
    private fun salaryRule() = RuleStore.get(context).save(name = "Salary", counterpartyContains = "",
        counterpartyExact = "XYZ SCHOOL", displayLabel = "XYZ SCHOOL", category = TransactionCategory.INCOME,
        direction = MoneyDirection.INCOME, preset = RulePreset.SALARY, startsPayCycle = true,
        accountId = AccountStore.DEFAULT_MPESA_ID, payWindowStartDay = 24, payWindowEndDay = 28)
    @Test fun receivingAccountsUseTwoColumnsAndRemainSelectable() {
        var selected = "a"
        compose.setContent { PesaTrackTheme(darkTheme = true) {
            SalaryAccountGrid(listOf(MoneyAccount("a", "M-Pesa", AccountType.MOBILE_WALLET),
                MoneyAccount("b", "Equity Bank Kenya", AccountType.BANK),
                MoneyAccount("c", "M-Shwari Lock Savings", AccountType.SAVINGS)), "a", true) { selected = it }
        } }
        val first = compose.onNodeWithTag("salary-account-a").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithTag("salary-account-b").fetchSemanticsNode().boundsInRoot
        val third = compose.onNodeWithTag("salary-account-c").fetchSemanticsNode().boundsInRoot
        assertEquals(first.top, second.top, 1f); assertTrue(second.left > first.left); assertTrue(third.top > first.top)
        compose.onNodeWithTag("salary-account-a").assertIsSelected()
        compose.onNodeWithTag("salary-account-b").performClick()
        assertEquals("b", selected)
    }
    @Test fun receiptDateOpensCalendarAndKeepsSelectedDay() {
        val original = at("2025-09-28"); var selected = 0L
        compose.setContent { PesaTrackTheme(darkTheme = false) { SalaryReceiptDateButton(original, true) { selected = it } } }
        compose.onNodeWithTag("salary-receipt-date").performClick()
        compose.onNodeWithText("Receipt date").assertIsDisplayed()
        compose.onNodeWithTag("salary-date-confirm").performClick()
        assertEquals(original, selected)
    }
    @Test fun salaryTabsRecordAndConfirmRemovalWithoutLosingSource() {
        val rule = salaryRule()
        compose.setContent { PesaTrackTheme { SalaryScreen(PaddingValues()) } }
        compose.waitUntil(10000) { compose.onNodeWithTag("salary-tab-Add salary").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled).not() }
        compose.onNodeWithTag("salary-tab-Add salary").performClick()
        compose.onNodeWithTag("salary-amount").performTextInput("50000")
        compose.onNodeWithTag("salary-receipt-date").performClick()
        compose.onNodeWithTag("salary-date-confirm").performClick()
        compose.onNodeWithTag("salary-record").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Remove salary entry").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Remove salary entry").performScrollTo().performClick()
        compose.onNodeWithText("Remove salary?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(1, SalaryReceiptRepository(context).receipts(rule.id).size)
        compose.onNodeWithContentDescription("Remove salary entry").performClick()
        compose.onNodeWithTag("salary-remove-confirm").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("No salary recorded yet").fetchSemanticsNodes().isNotEmpty() }
        assertNull(PayCycleStore.get(context).currentCycle())
        assertTrue(RuleStore.get(context).getById(rule.id)!!.enabled)
        compose.onNodeWithTag("salary-tab-Source").performScrollTo().performClick()
        compose.onNodeWithTag("salary-account-${AccountStore.DEFAULT_MPESA_ID}").assertIsSelected()
    }
}

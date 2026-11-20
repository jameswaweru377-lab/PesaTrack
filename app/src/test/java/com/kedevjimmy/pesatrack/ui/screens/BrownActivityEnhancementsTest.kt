package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.data.local.ActivitySummary
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class BrownActivityEnhancementsTest {
    @Test fun paymentBadgesReflectStoredMethodsWithoutGuessingFromMerchants() {
        assertEquals("Pay Bill", activityPaymentMethod(transaction("PAY_BILL")))
        assertEquals("Pay Bill", activityPaymentMethod(transaction("OTHER", "Equity Paybill Account for account")))
        assertEquals("Buy Goods", activityPaymentMethod(transaction("BUY_GOODS")))
        assertEquals("Send Money", activityPaymentMethod(transaction("SENT_MONEY")))
        assertEquals("Pochi", activityPaymentMethod(transaction("SENT_MONEY", "Pochi la Biashara - Vendor")))
        assertEquals("Pochi", activityPaymentMethod(transaction("OTHER", "Pochi la Biashara")))
        assertEquals("Send Money", activityPaymentMethod(transaction("SENT_MONEY", "Pochi shop")))
        assertEquals("Transfer", activityPaymentMethod(transaction("OTHER", direction = MoneyDirection.TRANSFER.name)))
        assertEquals("Bank to M-Pesa", activityPaymentMethod(transaction("OTHER", "Bank to M-Pesa transfer",
            direction = MoneyDirection.TRANSFER.name)))
        assertNull(activityPaymentMethod(transaction("OTHER", "Paybill Consulting Ltd")))
        assertNull(activityPaymentMethod(transaction("RECEIVED")))
    }

    @Test fun onlyRecordedPositiveFeesGetBadges() {
        assertEquals(500L, activityFeeBadgeAmount(transaction("PAY_BILL", feeMinor = 500L)))
        assertNull(activityFeeBadgeAmount(transaction("PAY_BILL", feeMinor = 0L)))
        assertNull(activityFeeBadgeAmount(transaction("PAY_BILL", feeMinor = -1L)))
    }

    @Test fun tickerMoneyOutIncludesRecordedFeesWithoutDoubleCountingSpending() {
        val onlyTransferFee = ActivitySummary(
            incomeMinor = 0L, spentMinor = 0L, feesMinor = 500L,
            transferMinor = 25_000L, netMinor = -500L, needsReviewCount = 0
        )
        assertEquals(500L, activityTickerMoneyOut(onlyTransferFee))
        assertEquals(10_500L, activityTickerMoneyOut(onlyTransferFee.copy(spentMinor = 10_000L)))
        assertNull(activityTickerMoneyOut(onlyTransferFee.copy(
            spentMinor = Long.MAX_VALUE, feesMinor = 1L
        )))
    }

    @Test fun tickerRangesFollowLocalCalendarAndNeverOverlapAtYearBoundary() {
        val kenya = TimeZone.getTimeZone("Africa/Nairobi")
        val now = at(kenya, 2026, Calendar.DECEMBER, 31, 23, 50)
        assertEquals(
            ActivityTickerRange(at(kenya, 2026, Calendar.JANUARY, 1),
                at(kenya, 2027, Calendar.JANUARY, 1)),
            activityTickerRange(ActivityTickerPeriod.YEAR, now, kenya)
        )
        assertEquals(
            ActivityTickerRange(at(kenya, 2026, Calendar.DECEMBER, 1),
                at(kenya, 2027, Calendar.JANUARY, 1)),
            activityTickerRange(ActivityTickerPeriod.MONTH, now, kenya)
        )
        assertEquals(
            ActivityTickerRange(at(kenya, 2026, Calendar.DECEMBER, 31),
                at(kenya, 2027, Calendar.JANUARY, 1)),
            activityTickerRange(ActivityTickerPeriod.TODAY, now, kenya)
        )
    }

    private fun at(zone: TimeZone, year: Int, month: Int, day: Int, hour: Int = 0,
                   minute: Int = 0): Long = Calendar.getInstance(zone).apply {
        clear()
        set(year, month, day, hour, minute)
    }.timeInMillis

    private fun transaction(type: String, counterparty: String? = null, feeMinor: Long = 0L,
                            direction: String = MoneyDirection.EXPENSE.name) = StoredTransaction(
        id = 1L, transactionCode = null, fingerprint = "synthetic", type = type,
        amountMinor = 10000L, feeMinor = feeMinor, balanceMinor = null,
        counterparty = counterparty, counterpartyPhone = null, occurredAtText = null,
        occurredAtEpochMs = 1L, receivedAtEpochMs = 1L, sourcePackage = null,
        sourceKind = "test", direction = direction, category = "OTHER",
        reviewState = "AUTO_CLASSIFIED", ruleId = null, planningMonth = null,
        reversalOfCode = null, accountId = null, linkedAccountId = null,
        payCycleId = null, cycleCandidateReason = null, cycleOverride = null
    )
}

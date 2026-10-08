package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityFinancialRouteLabelTest {
    @Test
    fun expenseShowsFundingInstitution() {
        assertEquals(
            "From M-Pesa",
            financialActivityRouteLabel(
                item =
                    transaction(
                        direction =
                            MoneyDirection
                                .EXPENSE
                                .name
                    ),
                accountName = "M-Pesa",
                linkedAccountName = null
            )
        )
    }

    @Test
    fun incomeShowsReceivingInstitution() {
        assertEquals(
            "To Equity Bank Kenya",
            financialActivityRouteLabel(
                item =
                    transaction(
                        direction =
                            MoneyDirection
                                .INCOME
                                .name
                    ),
                accountName =
                    "Equity Bank Kenya",
                linkedAccountName = null
            )
        )
    }

    @Test
    fun ownTransferShowsBothInstitutions() {
        assertEquals(
            "M-Pesa → KCB Bank Kenya",
            financialActivityRouteLabel(
                item =
                    transaction(
                        direction =
                            MoneyDirection
                                .TRANSFER
                                .name
                    ),
                accountName = "M-Pesa",
                linkedAccountName =
                    "KCB Bank Kenya"
            )
        )
    }

    private fun transaction(
        direction: String
    ) =
        StoredTransaction(
            id = 1L,
            transactionCode = null,
            fingerprint = "route-test",
            type = "OTHER",
            amountMinor = 100L,
            feeMinor = 0L,
            balanceMinor = null,
            counterparty = null,
            counterpartyPhone = null,
            occurredAtText = null,
            occurredAtEpochMs = 1L,
            receivedAtEpochMs = 1L,
            sourcePackage = null,
            sourceKind = "test",
            direction = direction,
            category = "OTHER",
            reviewState =
                "AUTO_CLASSIFIED",
            ruleId = null,
            planningMonth = null,
            reversalOfCode = null,
            accountId = null,
            linkedAccountId = null,
            payCycleId = null,
            cycleCandidateReason = null,
            cycleOverride = null
        )
}

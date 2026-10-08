package com.kedevjimmy.pesatrack.ui

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import com.kedevjimmy.pesatrack.ui.screens.ActivityScreen
import com.kedevjimmy.pesatrack.ui.screens.ActivityScreenCache
import com.kedevjimmy.pesatrack.ui.theme.PesaTrackTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityLiveRefreshTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @After
    fun tearDown() {
        compose.runOnIdle { }
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }

    @Test
    fun visibleActivityRefreshesWhenLedgerChanges() {
        val now = System.currentTimeMillis()
        insertExpense("RCINIT0001", "Initial merchant", now)

        compose.setContent {
            PesaTrackTheme {
                ActivityScreen(
                    scaffoldPadding = PaddingValues(),
                    stateCache = ActivityScreenCache()
                )
            }
        }

        compose.waitUntil(15_000L) {
            compose.onAllNodes(hasText("Initial merchant"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        insertExpense("RCFRESH0002", "Fresh merchant", now + 1_000L)
        LedgerUpdateBus.notifyChanged()

        compose.waitUntil(15_000L) {
            compose.onAllNodes(hasText("Fresh merchant"))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun insertExpense(
        code: String,
        counterparty: String,
        epochMs: Long
    ) {
        TransactionStore.get(context).insertIfNew(
            parsed = ParsedMpesaTransaction(
                transactionCode = code,
                type = MpesaTransactionType.SENT_MONEY,
                amountMinor = 125_000L,
                counterparty = counterparty,
                counterpartyPhone = null,
                feeMinor = 0L,
                balanceMinor = null,
                occurredAtText = null,
                rawMessage = code
            ),
            sourcePackage = null,
            sourceKind = "test",
            receivedAtEpochMs = epochMs,
            occurredAtEpochMs = epochMs,
            rebuildPayCycles = false,
            runRelationshipReconciliation = false,
            runMirrorCorrelation = false,
            directionOverride = MoneyDirection.EXPENSE,
            categoryOverride = TransactionCategory.OTHER,
            reviewStateOverride = ReviewState.AUTO_CLASSIFIED
        )
    }
}

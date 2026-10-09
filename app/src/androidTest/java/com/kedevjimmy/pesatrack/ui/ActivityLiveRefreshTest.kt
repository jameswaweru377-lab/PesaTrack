package com.kedevjimmy.pesatrack.ui

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
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
        val cache = ActivityScreenCache()

        compose.setContent {
            PesaTrackTheme {
                ActivityScreen(
                    scaffoldPadding = PaddingValues(),
                    stateCache = cache
                )
            }
        }

        compose.waitUntil(15_000L) {
            cache.data?.snapshot?.transactions?.any { it.counterparty == "Initial merchant" } == true
        }
        // LazyColumn does not compose rows below the summary on a compact viewport.
        val verticalList = SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex) and
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
        compose.onNode(verticalList)
            .performScrollToNode(hasText("Initial merchant"))
        compose.onNode(hasText("Initial merchant")).assertIsDisplayed()

        insertExpense("RCFRESH0002", "Fresh merchant", now + 1_000L)
        LedgerUpdateBus.notifyChanged()

        compose.waitUntil(15_000L) {
            cache.data?.snapshot?.transactions?.any { it.counterparty == "Fresh merchant" } == true
        }
        compose.onNode(verticalList)
            .performScrollToNode(hasText("Fresh merchant"))
        compose.onNode(hasText("Fresh merchant")).assertIsDisplayed()
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

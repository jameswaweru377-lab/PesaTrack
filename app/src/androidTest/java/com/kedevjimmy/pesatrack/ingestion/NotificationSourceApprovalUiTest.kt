package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.ui.screens.NotificationSourceApprovalSheet
import com.kedevjimmy.pesatrack.ui.screens.NotificationSourceApprovalContent
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationSourceApprovalUiTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var trust: NotificationSourceTrustStore
    @Before fun before() {
        context.getSharedPreferences(NotificationSourceTrustStore.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        trust = NotificationSourceTrustStore(context)
        trust.observe("ke.example.bank", "A Invented Bank")
    }
    @After fun after() {
        context.getSharedPreferences(NotificationSourceTrustStore.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }
    @Test fun settingsApprovalTogglePersistsAndRevokesExactSource() {
        compose.setContent { MaterialTheme { NotificationSourceApprovalSheet(onDismiss = {}) } }
        val toggle = compose.onNodeWithContentDescription("Approve financial notifications from A Invented Bank")
        toggle.assertIsOff().performClick().assertIsOn()
        compose.runOnIdle { assertTrue(trust.isApproved("ke.example.bank")) }
        toggle.performClick().assertIsOff()
        compose.runOnIdle { assertFalse(trust.isApproved("ke.example.bank")) }
    }

    @Test fun expandingAvailableAppsDoesNotApproveSources() {
        val hiddenSource = ObservedNotificationSource("ke.example.reader", "Sample Reader")
        compose.setContent {
            MaterialTheme {
                NotificationSourceApprovalContent(availableSources = listOf(hiddenSource), onDismiss = {})
            }
        }
        val toggle = compose.onNodeWithContentDescription("Approve financial notifications from Sample Reader")
        toggle.assertDoesNotExist()
        compose.onNodeWithText("Show all available apps").performClick()
        toggle.assertIsOff()
        compose.runOnIdle { assertFalse(trust.isApproved(hiddenSource.packageName)) }
        compose.onNodeWithText("Show suggested apps").performClick()
        toggle.assertDoesNotExist()
        compose.onNodeWithText("Show all available apps").performClick()
        toggle.assertIsOff().performClick().assertIsOn()
        compose.runOnIdle { assertTrue(trust.isApproved(hiddenSource.packageName)) }
    }
}

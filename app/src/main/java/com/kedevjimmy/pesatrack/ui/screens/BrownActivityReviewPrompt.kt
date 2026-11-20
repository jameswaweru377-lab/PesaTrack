package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone

/** A day is the device-local financial day, not a UTC timer boundary. */
internal fun activityReviewDayKey(nowMs: Long, zone: TimeZone = TimeZone.getDefault()): Int =
    Calendar.getInstance(zone).apply { timeInMillis = nowMs }.let { calendar ->
        calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
    }

internal fun shouldOfferActivityReviewPrompt(
    pendingCount: Int, todayKey: Int, lastShownKey: Int, lastShownCount: Int
): Boolean = pendingCount > 0 &&
    (todayKey != lastShownKey || pendingCount > lastShownCount)

/** Shows after PIN unlock and only for newly relevant local review attention. */
@Composable
internal fun BrownActivityHomeReviewPrompt(onReview: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val prefs = remember(context) {
        context.getSharedPreferences("activity_review_prompt", Context.MODE_PRIVATE)
    }
    val unlocked = LocalFinancialContentUnlocked.current
    val revision by LedgerUpdateBus.state.collectAsState()
    var pendingCount by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(unlocked, revision.revision) {
        if (!unlocked) {
            visible = false
            return@LaunchedEffect
        }
        delay(1_200L)
        val freshCount = try {
            activityPendingReviewCount(readActivitySupplementalData(context))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Log.w("PesaTrackActivity", "Review reminder unavailable")
            return@LaunchedEffect
        }
        val today = activityReviewDayKey(System.currentTimeMillis())
        if (shouldOfferActivityReviewPrompt(freshCount, today,
                prefs.getInt("shown_day", -1), prefs.getInt("shown_count", 0))) {
            pendingCount = freshCount
            prefs.edit().putInt("shown_day", today).putInt("shown_count", freshCount).apply()
            visible = true
        } else if (freshCount == 0) {
            visible = false
        }
    }
    if (unlocked && visible) BrownActivityReviewDialog(pendingCount,
        onDismiss = { visible = false }, onReview = {
            visible = false
            onReview()
        })
}

@Composable
private fun BrownActivityReviewDialog(count: Int, onDismiss: () -> Unit, onReview: () -> Unit) {
    val p = homeColors()
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = p.cream, shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(color = p.white,
                    shape = RoundedCornerShape(PesaDesignTokens.iconRadius),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = p.coffee.copy(alpha = .12f), shape = CircleShape) {
                            Box(Modifier.size(PesaDesignTokens.minimumTouchTarget),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Rounded.FactCheck, null,
                                    Modifier.size(24.dp), tint = p.coffee)
                            }
                        }
                        Text("A quick review", fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, color = p.ink)
                    }
                }
                Text("${if (count == 1) "1 item needs" else "$count items need"} your review. " +
                    "You can check them now or come back later.",
                    color = p.ink, fontSize = 14.sp, textAlign = TextAlign.Center)
                Button(onClick = onReview, modifier = Modifier.fillMaxWidth().heightIn(
                    min = PesaDesignTokens.minimumTouchTarget),
                    shape = RoundedCornerShape(PesaDesignTokens.iconRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = p.coffee,
                        contentColor = MaterialTheme.colorScheme.onPrimary)) {
                    Text("Review now", fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(
                    min = PesaDesignTokens.minimumTouchTarget)) {
                    Text("Not now", color = p.muted)
                }
            }
        }
    }
}

package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.data.local.InvestmentEventType
import com.kedevjimmy.pesatrack.data.local.ObservedInvestmentEvent
import com.kedevjimmy.pesatrack.data.local.ObservedInvestmentPosition
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date

private val brown = Color(0xFF552510)
private val secondaryBrown = Color(0xFF846554)
private val goldOutline = Color(0xFFDEC39E)

/** An unrecognised security gets a text fallback, never a guessed company logo. */
internal fun investmentBrandDrawable(label: String): Int? = when {
    label.equals("SCOM", true) || label.contains("Safaricom", true) -> R.drawable.investment_safaricom_mark
    label.equals("KPLC", true) || label.contains("Kenya Power", true) -> R.drawable.investment_kenya_power_mark
    label.contains("Ziidi", true) || label.equals("ZIIDI", true) -> R.drawable.investment_ziidi_mark
    else -> null
}

internal fun investmentEventTitle(event: ObservedInvestmentEvent): String = when (event.type) {
    InvestmentEventType.ORDER_SUBMITTED -> "Order submitted"
    InvestmentEventType.BUY_EXECUTED -> "Purchase confirmed"
    InvestmentEventType.SELL_EXECUTED -> "Sale confirmed"
}

@Composable
internal fun InvestmentPortfolioContent(
    padding: PaddingValues,
    positions: List<ObservedInvestmentPosition>,
    events: List<ObservedInvestmentEvent>,
    submittedOrders: Int,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(padding).fillMaxSize().background(BrownHomePalette.canvas),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = brown)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Investments", style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = brown)
                    Text("YOUR MONEY, YOUR PLANS", style = MaterialTheme.typography.labelSmall,
                        color = secondaryBrown)
                }
                Spacer(Modifier.width(48.dp))
            }
        }
        item { InvestmentGrowthHero() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InvestmentMetric("${positions.size}", "Observed holdings", Modifier.weight(1f))
                InvestmentMetric("$submittedOrders",
                    "Orders submitted", Modifier.weight(1f))
            }
        }
        item {
            Text("Your holdings", style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = brown)
        }
        if (positions.isEmpty()) item {
            InvestmentNotice("No confirmed investment units found yet. Submitted orders and IPO applications are not holdings.")
        }
        items(positions, key = { "holding:${it.label}" }) { position ->
            InvestmentHoldingRow(position)
        }
        item {
            Text("Investment history", style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = brown)
        }
        if (events.isEmpty()) item {
            InvestmentNotice("Purchases and submitted orders will appear here when supported messages are recognised.")
        }
        items(events) { event ->
            InvestmentEventRow(event)
        }
        item {
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Info, null, tint = secondaryBrown, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Offline record only. Units reflect observed executions, not verified brokerage balances. Market values, dividends and IPO allotments remain unavailable until supported evidence is confirmed.",
                    style = MaterialTheme.typography.bodySmall, color = secondaryBrown)
            }
        }
    }
}

@Composable
private fun InvestmentGrowthHero() {
    Box(Modifier.fillMaxWidth().height(188.dp).clip(RoundedCornerShape(24.dp))) {
        Image(painterResource(R.drawable.investment_growth_hero), null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(Modifier.align(Alignment.CenterStart).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Your investments", color = Color.White,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Value unavailable", color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            Text("No live market connection", color = Color.White.copy(alpha = .9f),
                style = MaterialTheme.typography.bodySmall)
            Surface(color = Color(0xFF8C5031), shape = RoundedCornerShape(12.dp)) {
                Text("OFFLINE", Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun InvestmentMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color.White, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, goldOutline)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, color = brown, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Text(label, color = secondaryBrown, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun InvestmentHoldingRow(position: ObservedInvestmentPosition) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, goldOutline)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            InvestmentBrandMark(position.label)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(investmentCompanyName(position.label), color = brown,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${investmentTicker(position.label)} • Observed units", color = secondaryBrown,
                    style = MaterialTheme.typography.bodySmall)
                Text("Cost unavailable", color = secondaryBrown,
                    style = MaterialTheme.typography.labelSmall)
            }
            Text(NumberFormat.getNumberInstance(Locale.US).format(
                position.quantityMicrounits / 1_000_000.0),
                color = brown, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InvestmentBrandMark(label: String) {
    val logo = investmentBrandDrawable(label)
    Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
        .background(Color(0xFFFCF6F0)), contentAlignment = Alignment.Center) {
        if (logo != null) {
            Image(painterResource(logo), contentDescription = "${investmentCompanyName(label)} logo",
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        } else {
            Text(investmentTicker(label), color = brown,
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InvestmentEventRow(event: ObservedInvestmentEvent) {
    val locale = LocalConfiguration.current.locales[0]
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, goldOutline)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            InvestmentBrandMark(event.instrumentLabel)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(investmentEventTitle(event), color = brown, fontWeight = FontWeight.SemiBold)
                Text("${event.instrumentLabel} • ${SimpleDateFormat("d MMM yyyy", locale).format(Date(event.occurredAtEpochMs))}",
                    color = secondaryBrown, style = MaterialTheme.typography.bodySmall)
            }
            Text(event.valueMinor?.let(::formatKesAmount) ?: "Amount unknown",
                color = brown, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun InvestmentNotice(message: String) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, goldOutline)) {
        Text(message, Modifier.padding(16.dp), color = secondaryBrown,
            style = MaterialTheme.typography.bodyMedium)
    }
}

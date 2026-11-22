package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.planning.LocalInboxNotice
import com.kedevjimmy.pesatrack.planning.LocalNotificationInboxPolicy
import com.kedevjimmy.pesatrack.planning.LocalNotificationInboxStore
import com.kedevjimmy.pesatrack.planning.NotificationInboxDisplayPolicy
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

/** A history of locally delivered reminders; financial SMS contents are never shown. */
@Composable
fun NotificationCenterScreen(
    padding: PaddingValues,
    onBack: () -> Unit,
    onOpen: (String) -> Unit
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val revision by LocalNotificationInboxStore.revision.collectAsState()
    val alerts = remember(context, revision) { LocalNotificationInboxStore.list(context) }
    var unreadOnly by rememberSaveable { mutableStateOf(false) }
    val unreadCount = LocalNotificationInboxPolicy.unreadCount(alerts)
    val timezone = TimeZone.getDefault()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { now = System.currentTimeMillis() }
    LaunchedEffect(timezone.id) {
        while (true) {
            val tomorrow = Calendar.getInstance(timezone).apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            delay((tomorrow - System.currentTimeMillis()).coerceAtLeast(1_000L))
            now = System.currentTimeMillis()
        }
    }
    val calendarDay = Calendar.getInstance(timezone).apply { timeInMillis = now }
    val dayKey = calendarDay.get(Calendar.YEAR) * 1000 + calendarDay.get(Calendar.DAY_OF_YEAR)
    val display = remember(alerts, unreadOnly, timezone, dayKey) {
        NotificationInboxDisplayPolicy.present(alerts, unreadOnly, now, timezone)
    }
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(colors.background).padding(padding)
        .padding(horizontal = PesaDesignTokens.screenHorizontalPadding)) {
        NotificationInboxHeader(onBack = onBack, onSettings = { onOpen("settings") })
        NotificationInboxFilters(unreadOnly, unreadCount,
            onSelect = { unreadOnly = it },
            onMarkAllRead = { LocalNotificationInboxStore.markAllRead(context) })
        if (display.visibleCount == 0) {
            NotificationInboxEmpty(unreadOnly,
                onAction = { if (unreadOnly) unreadOnly = false else onBack() })
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = PesaDesignTokens.sectionGap)) {
                display.featured?.let { alert ->
                    item("featured-date") {
                        Text(SimpleDateFormat("EEEE d MMM yyyy", locale).apply {
                            timeZone = timezone
                        }.format(Date(alert.createdAt)),
                            modifier = Modifier.padding(vertical = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant)
                    }
                    item("featured-${alert.key}") {
                        FeaturedReviewNotice(alert) {
                            LocalNotificationInboxStore.markRead(context, alert.key)
                            onOpen(alert.route)
                        }
                    }
                }
                if (display.today.isNotEmpty()) {
                    item("today-title") { InboxSectionTitle("Today") }
                    items(display.today, key = { it.key }) { alert ->
                        NotificationInboxRow(alert, now, timezone, locale) {
                            LocalNotificationInboxStore.markRead(context, alert.key)
                            onOpen(alert.route)
                        }
                    }
                }
                if (display.earlierThisWeek.isNotEmpty()) {
                    item("week-title") { InboxSectionTitle("Earlier this week") }
                    items(display.earlierThisWeek, key = { it.key }) { alert ->
                        NotificationInboxRow(alert, now, timezone, locale) {
                            LocalNotificationInboxStore.markRead(context, alert.key)
                            onOpen(alert.route)
                        }
                    }
                }
                if (display.older.isNotEmpty()) {
                    item("older-title") { InboxSectionTitle("Earlier") }
                    items(display.older, key = { it.key }) { alert ->
                        NotificationInboxRow(alert, now, timezone, locale) {
                            LocalNotificationInboxStore.markRead(context, alert.key)
                            onOpen(alert.route)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationInboxHeader(onBack: () -> Unit, onSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = PesaDesignTokens.minimumTouchTarget),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = colors.primary)
        }
        Box(Modifier.width(1.dp).height(24.dp).background(colors.outlineVariant))
        Spacer(Modifier.width(8.dp))
        Image(painterResource(R.drawable.ic_pesatrack_home_mark), contentDescription = null,
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(PesaDesignTokens.iconRadius)))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.width(1.dp).height(24.dp).background(colors.outlineVariant))
        Spacer(Modifier.width(8.dp))
        Text("Notifications", Modifier.weight(1f), maxLines = 1,
            overflow = TextOverflow.Ellipsis, color = colors.onBackground,
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "More notification options")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Notification settings") }, onClick = {
                    menuOpen = false
                    onSettings()
                })
            }
        }
    }
}

@Composable
private fun NotificationInboxFilters(unreadOnly: Boolean, unreadCount: Int,
                                     onSelect: (Boolean) -> Unit, onMarkAllRead: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InboxFilterChip("All", selected = !unreadOnly) { onSelect(false) }
        InboxFilterChip("Unread ($unreadCount)", selected = unreadOnly) { onSelect(true) }
        Spacer(Modifier.weight(1f))
        if (unreadCount > 0) {
            TextButton(onClick = onMarkAllRead,
                contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text("Mark all read", maxLines = 1, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun InboxFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick,
        modifier = Modifier.heightIn(min = PesaDesignTokens.minimumTouchTarget),
        shape = RoundedCornerShape(PesaDesignTokens.controlRadius),
        color = if (selected) colors.primary else colors.surfaceVariant) {
        Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) colors.onPrimary else colors.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FeaturedReviewNotice(notice: LocalInboxNotice, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, color = colors.surfaceVariant,
        shape = RoundedCornerShape(PesaDesignTokens.cardRadius)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NotificationIcon(Icons.Rounded.FactCheck, colors.primary, Modifier.size(48.dp))
            Box(Modifier.width(1.dp).height(48.dp).background(colors.primary.copy(alpha = .65f)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("NEEDS ATTENTION", color = colors.tertiary,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                Text(notice.title, color = colors.onSurface, fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(notice.summary, color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null,
                    modifier = Modifier.size(24.dp), tint = colors.primary)
                Text("Review now", color = colors.primary,
                    style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun InboxSectionTitle(label: String) {
    Text(label, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        style = MaterialTheme.typography.headlineSmall,
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun NotificationInboxRow(notice: LocalInboxNotice, now: Long,
                                 timezone: TimeZone, locale: Locale, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val day = Calendar.getInstance(timezone).apply { timeInMillis = notice.createdAt }
    val current = Calendar.getInstance(timezone).apply { timeInMillis = now }
    val yesterday = Calendar.getInstance(timezone).apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, -1)
    }
    fun sameDay(a: Calendar, b: Calendar) = a.get(Calendar.ERA) == b.get(Calendar.ERA) &&
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    fun format(pattern: String) = SimpleDateFormat(pattern, locale).apply {
        timeZone = timezone
    }.format(Date(notice.createdAt))
    val timestamp = when {
        sameDay(day, current) -> format("h:mm a").lowercase(locale)
        sameDay(day, yesterday) -> "Yesterday"
        notice.createdAt < now && now - notice.createdAt < 7L * 24 * 60 * 60 * 1000 ->
            format("EEE")
        else -> format("d MMM")
    }
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().heightIn(min = PesaDesignTokens.minimumTouchTarget)
            .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NotificationIcon(noticeIcon(notice), colors.primary, Modifier.size(48.dp))
            Box(Modifier.width(1.dp).height(32.dp)
                .background(colors.outlineVariant.copy(alpha = .65f)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(notice.title, Modifier.weight(1f), maxLines = 2,
                        overflow = TextOverflow.Ellipsis, color = colors.onSurface,
                        fontWeight = if (notice.read) FontWeight.Medium else FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium)
                    Text(timestamp, color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    if (!notice.read) Box(Modifier.size(8.dp).background(colors.primary, CircleShape))
                }
                Text(notice.summary, color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    HorizontalDivider(color = colors.outlineVariant.copy(alpha = PesaDesignTokens.faintDividerAlpha))
}

@Composable
private fun NotificationIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.clip(RoundedCornerShape(PesaDesignTokens.iconRadius))
        .background(colors.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

private fun noticeIcon(notice: LocalInboxNotice): ImageVector = when (notice.category.uppercase(Locale.ROOT)) {
    "ACTIVITY" -> Icons.Rounded.FactCheck
    "ACCOUNTS", "ACCOUNT" -> Icons.Rounded.AccountBalance
    "BILLS" -> Icons.Rounded.CalendarMonth
    "BACKUP" -> Icons.Rounded.VerifiedUser
    "BUDGET" -> Icons.Rounded.Savings
    "SAVINGS" -> Icons.Rounded.Flag
    "INCOME", "SALARY" -> Icons.Rounded.AccountBalanceWallet
    "DEBT" -> Icons.Rounded.PeopleOutline
    else -> Icons.Rounded.NotificationsNone
}

@Composable
private fun NotificationInboxEmpty(unreadOnly: Boolean, onAction: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.NotificationsNone, contentDescription = null,
            tint = colors.primary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(if (unreadOnly) "You're all caught up" else "No notifications yet",
            color = colors.onBackground, style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(if (unreadOnly) "You have no unread reminders."
            else "Reminders from PesaTrack will appear here.",
            color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onAction) { Text(if (unreadOnly) "View all" else "Go back") }
    }
}

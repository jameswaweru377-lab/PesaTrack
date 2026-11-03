package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.DebtAccountSnapshot
import com.kedevjimmy.pesatrack.data.local.DebtBalanceQuality
import com.kedevjimmy.pesatrack.data.local.PaydayBalanceQuality
import com.kedevjimmy.pesatrack.domain.model.AccountType
import com.kedevjimmy.pesatrack.domain.rules.AccountIdentityPresentation
import com.kedevjimmy.pesatrack.ui.theme.Amber
import com.kedevjimmy.pesatrack.ui.theme.AmberSoft
import com.kedevjimmy.pesatrack.ui.theme.AquaSoft
import com.kedevjimmy.pesatrack.ui.theme.AuroraAqua
import com.kedevjimmy.pesatrack.ui.theme.PesaDesignTokens
import com.kedevjimmy.pesatrack.ui.theme.Rose
import com.kedevjimmy.pesatrack.ui.theme.RoseSoft
import com.kedevjimmy.pesatrack.ui.theme.Sky
import com.kedevjimmy.pesatrack.ui.theme.SkySoft
import com.kedevjimmy.pesatrack.ui.theme.VioletSoft

/** All marks are rendered locally; the app never fetches provider imagery. */
@Composable
internal fun AccountProviderMark(name: String, modifier: Modifier = Modifier) {
    val lower = name.lowercase()
    val (label, bg, ink) = when {
        "m-pesa" in lower || "mpesa" in lower -> Triple("M-PESA", AquaSoft, AuroraAqua)
        "equity" in lower -> Triple("EQUITY", RoseSoft, Rose)
        "kcb" in lower -> Triple("KCB", SkySoft, Sky)
        "co-operative" in lower || "co-op" in lower -> Triple("COOP", AquaSoft, AuroraAqua)
        "absa" in lower -> Triple("absa", RoseSoft, Rose)
        "i&m" in lower -> Triple("I&M", SkySoft, Sky)
        "cash" in lower -> Triple("▣", AquaSoft, AuroraAqua)
        "lock" in lower -> Triple("▤", VioletSoft, MaterialTheme.colorScheme.primary)
        "shwari" in lower -> Triple("◇", VioletSoft, MaterialTheme.colorScheme.primary)
        "branch" in lower || "loan" in lower -> Triple("↗", AmberSoft, Amber)
        else -> Triple("▤", VioletSoft, MaterialTheme.colorScheme.primary)
    }
    Surface(modifier = modifier.size(46.dp), shape = RoundedCornerShape(15.dp), color = bg) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = ink,
                style = if (label.length > 3) MaterialTheme.typography.labelSmall
                    else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}

@Composable
internal fun AccountsSectionTitle(label: String) {
    Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
}

internal data class AccountDisplayLine(
    val name: String, val accountId: String?, val type: AccountType,
    val balanceMinor: Long?, val quality: PaydayBalanceQuality?
)

/** Keep separately selected M-Shwari and Lock Savings visible, never merge their unknowns. */
internal fun accountDisplayLines(entries: List<AccountBalancePresentation>): List<AccountDisplayLine> =
    entries.flatMap { entry ->
        val group = entry.group
        if (group.displayName.equals("M-Shwari", ignoreCase = true) && entry.components.size > 1) {
            entry.components.map { component ->
                val locked = component.label.contains("lock", ignoreCase = true)
                val account = group.accounts.firstOrNull {
                    it.name.contains("lock", ignoreCase = true) == locked
                }
                AccountDisplayLine(component.label, account?.id, AccountType.SAVINGS,
                    component.balanceMinor, component.quality)
            }
        } else {
            listOf(AccountDisplayLine(AccountIdentityPresentation.title(group),
                group.accounts.firstOrNull()?.id, group.type, entry.balanceMinor, entry.quality))
        }
    }

@Composable
internal fun DebtDirectionSelector(owedToMe: Boolean, onIowe: () -> Unit, onOwedToMe: () -> Unit) {
    Surface(shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(false to "I owe", true to "Owed to me").forEach { (isReceivable, label) ->
                Surface(onClick = if (isReceivable) onOwedToMe else onIowe,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (owedToMe == isReceivable) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = if (owedToMe == isReceivable) 1.dp else 0.dp) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (owedToMe == isReceivable) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
internal fun AccountSectionGroup(
    entries: List<AccountBalancePresentation>,
    amountsHidden: Boolean,
    onAccountClick: ((String) -> Unit)?
) {
    Surface(shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
        border = BorderStroke(PesaDesignTokens.cardOutlineWidth, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
            val lines = accountDisplayLines(entries)
            lines.forEachIndexed { index, line ->
                AccountSectionRow(line, amountsHidden, onAccountClick)
                if (index != lines.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun AccountSectionRow(
    line: AccountDisplayLine,
    hidden: Boolean,
    onAccountClick: ((String) -> Unit)?
) {
    val quality = when (line.quality) {
        PaydayBalanceQuality.OBSERVED -> "Observed"
        PaydayBalanceQuality.RECONSTRUCTED -> "Estimated"
        PaydayBalanceQuality.USER_ENTERED -> "Entered"
        else -> ""
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 80.dp)
        .clickable(enabled = line.accountId != null && onAccountClick != null) {
            line.accountId?.let { onAccountClick?.invoke(it) }
        }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        AccountProviderMark(line.name)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(line.name, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(when (line.type) {
                AccountType.BANK -> "Bank"
                AccountType.MOBILE_WALLET -> "Wallet"
                AccountType.SAVINGS -> "Savings"
                AccountType.CASH -> "Manual account"
                else -> "Account"
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(if (hidden) "••••••" else line.balanceMinor?.let(::formatKesAmount)
                ?: "Balance unavailable", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (line.balanceMinor != null && !hidden) Text(quality,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = "Open account",
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp).size(18.dp))
    }
}

@Composable
internal fun CompactDebtAccountRow(debt: DebtAccountSnapshot) {
    val unknown = debt.balanceQuality == DebtBalanceQuality.UNKNOWN ||
        (debt.hasUnresolvedDrawdown && debt.outstandingMinor == 0L &&
            debt.balanceQuality == DebtBalanceQuality.ESTIMATED)
    Surface(shape = RoundedCornerShape(PesaDesignTokens.innerRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(PesaDesignTokens.cardOutlineWidth, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            AccountProviderMark(debt.name)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(debt.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(when {
                    unknown -> "Balance needs confirmation"
                    debt.isSettled -> "Settled"
                    debt.needsSettlementConfirmation -> "Confirmation pending"
                    debt.balanceQuality == DebtBalanceQuality.OBSERVED -> "Observed balance"
                    else -> "Estimated from activity"
                }, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (unknown) "Unknown" else formatKesAmount(debt.outstandingMinor),
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                debt.lastRepaymentMinor?.let {
                    Text("Last paid ${formatKesAmount(it)}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun CompactDebtAccountGroup(debts: List<DebtAccountSnapshot>, onConfirmBalance: (() -> Unit)?) {
    Surface(shape = RoundedCornerShape(PesaDesignTokens.cardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(PesaDesignTokens.cardOutlineWidth, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
            debts.forEachIndexed { index, debt ->
                val unknown = debt.balanceQuality == DebtBalanceQuality.UNKNOWN ||
                    (debt.hasUnresolvedDrawdown && debt.outstandingMinor == 0L &&
                        debt.balanceQuality == DebtBalanceQuality.ESTIMATED)
                Row(Modifier.fillMaxWidth().heightIn(min = 82.dp), verticalAlignment = Alignment.CenterVertically) {
                    AccountProviderMark(debt.name)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(debt.name, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text(if (unknown) "Balance needs confirmation" else
                            if (debt.balanceQuality == DebtBalanceQuality.OBSERVED) "Observed" else "Estimated",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(if (unknown) "Unknown" else formatKesAmount(debt.outstandingMinor),
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        color = if (unknown) Amber else MaterialTheme.colorScheme.onSurface)
                }
                if (index < debts.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (onConfirmBalance != null) {
                OutlinedButton(onClick = onConfirmBalance, modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 8.dp).heightIn(min = 48.dp)) {
                    Text("Confirm balance")
                }
            }
        }
    }
}

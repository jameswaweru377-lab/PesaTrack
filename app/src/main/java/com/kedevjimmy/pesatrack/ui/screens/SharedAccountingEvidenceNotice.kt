package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.StatementMatchDecisionStore
import com.kedevjimmy.pesatrack.domain.statements.SharedAccountingViewPolicy
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A link to annual review evidence, never a replacement for month-scoped financial figures. */
@Composable
internal fun SharedAccountingEvidenceNotice(year: Int, revision: Long, onOpen: () -> Unit) {
    val context = LocalContext.current
    var evidence by remember(year) { mutableStateOf<SharedAccountingViewPolicy.View?>(null) }
    LaunchedEffect(year, revision) {
        evidence = null
        try {
            evidence = withContext(Dispatchers.IO) {
                StatementMatchDecisionStore(context).read(year).sharedAccounting
            }
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { evidence = null }
    }
    val available = evidence ?: return
    if (available.confirmedInternalTransfers == 0 && available.unresolvedEvidenceCount == 0) return
    PesaCard {
        Column(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Annual accounting evidence · $year", fontWeight = FontWeight.SemiBold)
            Text("${available.confirmedInternalTransfers} reviewed own-account transfers · " +
                "at least ${available.unresolvedEvidenceCount} evidence items need review",
                style = MaterialTheme.typography.bodySmall)
            Text("Review evidence in Statements. Your displayed income, spending and balances are unchanged.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

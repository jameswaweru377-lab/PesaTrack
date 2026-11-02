package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.statements.SharedAccountingViewPolicy
import com.kedevjimmy.pesatrack.ui.components.PesaCard

/** Separate evidence scopes; never replaces the regular Reports totals. */
@Composable
internal fun SharedAccountingReportCard(evidence: SharedAccountingViewPolicy.View?) {
    PesaCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Accounting evidence · annual review", fontWeight = FontWeight.SemiBold)
            Text("Activity and verified Statements use different accounting scopes.",
                style = MaterialTheme.typography.bodySmall)
            if (evidence == null) {
                Text("Annual comparison is unavailable. Existing Reports figures are unchanged.",
                    style = MaterialTheme.typography.bodySmall)
            } else {
                fun money(amount: Long?): String = amount?.let(::formatKesAmount) ?: "Unavailable"
                Text("Activity income: ${money(evidence.activityIncomeMinor)}")
                Text("Activity spending: ${money(evidence.activitySpendingMinor)}")
                Text("Verified Statement cash in: ${money(evidence.statementGrossInMinor)}")
                Text("Verified Statement cash out: ${money(evidence.statementGrossOutMinor)}")
                Text("Reviewed own-account transfers: ${money(evidence.reviewedInternalTransfersMinor)}")
                Text(if (evidence.status == SharedAccountingViewPolicy.Status.REVIEW_REQUIRED)
                    "Evidence requires review; this is not a reconciled balance."
                else "Evidence scopes remain separate; this is not a reconciled balance.",
                    style = MaterialTheme.typography.bodySmall)
            }
            Text("Read-only preview. No income, spending, account balance or report total is changed.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

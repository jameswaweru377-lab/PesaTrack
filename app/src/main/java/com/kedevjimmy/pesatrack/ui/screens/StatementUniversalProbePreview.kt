package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.statements.NativeBankProbeResult
import com.kedevjimmy.pesatrack.domain.statements.NativeProbeBlockReason
import com.kedevjimmy.pesatrack.ui.components.PesaCard

/** No save action: provisional table recognition has no issuer or archive authority. */
@Composable
internal fun StatementUniversalProbePreview(result: NativeBankProbeResult, onDismiss: () -> Unit) {
    PesaCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Unfamiliar statement preview", style = MaterialTheme.typography.titleMedium)
            when (result) {
                is NativeBankProbeResult.Provisional -> {
                    val candidate = result.result
                    Text("Recognized ${candidate.sourceRows} provisional rows across " +
                        "${candidate.tables.map { it.pageNumber }.distinct().size} table pages.")
                    Text("The source's issuer, account, boundaries and printed totals have not " +
                        "been independently verified.", style = MaterialTheme.typography.bodySmall)
                }
                is NativeBankProbeResult.Blocked -> {
                    Text(when (result.reason) {
                        NativeProbeBlockReason.UNSUPPORTED_DEVICE ->
                            "Native PDF text inspection requires Android 15 or newer."
                        NativeProbeBlockReason.INCOMPLETE_GEOMETRY ->
                            "Text positions or pages were incomplete. No partial rows were kept."
                        NativeProbeBlockReason.EMPTY_OR_SCANNED_DOCUMENT ->
                            "This PDF has no complete selectable text. Scanned PDF reading is not available yet."
                        NativeProbeBlockReason.UNRECOGNIZED_LAYOUT ->
                            "The complete table layout could not be recognized on this device."
                        NativeProbeBlockReason.UNVERIFIED_MOVEMENTS ->
                            "The table was read, but its cash movements did not fully reconcile."
                        NativeProbeBlockReason.PDF_READ_FAILURE ->
                            "This PDF could not be inspected completely on this device."
                    },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    result.pageNumber?.let { page ->
                        Text("First affected page: $page", style = MaterialTheme.typography.bodySmall)
                    }
                    result.safeDetail?.let { detail ->
                        Text("Native PDF check: $detail", style = MaterialTheme.typography.bodySmall)
                    }
                    if (result.reason == NativeProbeBlockReason.INCOMPLETE_GEOMETRY) {
                        Text("The PDF has selectable text, but Android did not supply enough " +
                            "trusted position evidence to reconstruct every transaction. " +
                            "You can report the page and check above without sharing bank details.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text("Not saved — verification incomplete",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Dismiss preview")
            }
        }
    }
}

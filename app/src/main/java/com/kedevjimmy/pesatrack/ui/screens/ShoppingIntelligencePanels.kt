package com.kedevjimmy.pesatrack.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kedevjimmy.pesatrack.domain.planning.ShoppingIntelligencePolicy
import com.kedevjimmy.pesatrack.domain.planning.ShoppingList
import com.kedevjimmy.pesatrack.domain.planning.ShoppingListPolicy
import com.kedevjimmy.pesatrack.domain.planning.ShoppingScanCandidate
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

/** Suggestions are local planning hints; their previous prices are never treated as current. */
@Composable
internal fun ShoppingIntelligenceActions(
    selected: ShoppingList,
    allLists: List<ShoppingList>,
    busy: Boolean,
    onScan: () -> Unit,
    onSave: (ShoppingList) -> Unit
) {
    val suggestions = remember(allLists, selected) {
        ShoppingIntelligencePolicy.suggest(allLists, selected)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onScan, enabled = !busy && selected.items.size < ShoppingListPolicy.MAX_ITEMS,
            colors = ButtonDefaults.buttonColors(containerColor = BrownHomePalette.coffee)) {
            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
            Text(" Scan shopping photo")
        }
        Text("Photo text stays on this phone. Review all items and prices before adding.",
            style = MaterialTheme.typography.bodySmall, color = BrownHomePalette.muted)
        if (suggestions.isNotEmpty()) {
            Surface(color = BrownHomePalette.white, shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BrownHomePalette.outline)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Suggested from previous lists", fontWeight = FontWeight.SemiBold,
                        color = BrownHomePalette.ink)
                    suggestions.forEach { suggestion ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(suggestion.name, style = MaterialTheme.typography.bodyMedium)
                                Text("Seen in ${suggestion.monthsSeen} month(s)" +
                                    (suggestion.previousUnitPriceMinor?.let {
                                        " · last estimate ${shoppingHintMoney(it)}"
                                    } ?: ""), style = MaterialTheme.typography.bodySmall,
                                    color = BrownHomePalette.muted)
                            }
                            TextButton(onClick = {
                                val item = ShoppingIntelligencePolicy.proposedSuggestionItem(
                                    suggestion, UUID.randomUUID().toString())
                                onSave(selected.copy(items = selected.items + item))
                            }, enabled = !busy && selected.items.size < ShoppingListPolicy.MAX_ITEMS) {
                                Text("Add")
                            }
                        }
                    }
                    Text("Suggestions start with an unknown price; update it for this month.",
                        style = MaterialTheme.typography.bodySmall, color = BrownHomePalette.muted)
                }
            }
        }
    }
}

/** Counts checked plans, not payments or confirmed purchases. */
@Composable
internal fun ShoppingHistoryPanel(lists: List<ShoppingList>) {
    if (lists.isEmpty()) return
    val months = remember(lists) { ShoppingIntelligencePolicy.history(lists).take(6) }
    Surface(color = BrownHomePalette.cream, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Shopping planning history", style = MaterialTheme.typography.titleMedium,
                color = BrownHomePalette.coffee, fontWeight = FontWeight.Bold)
            months.forEach { summary ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${summary.month} · ${summary.listCount} list(s) · " +
                        "${summary.checkedCount}/${summary.itemCount} checked",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Known estimated items: ${shoppingHintMoney(summary.knownEstimateMinor)}" +
                        if (summary.missingPriceCount > 0)
                            " · ${summary.missingPriceCount} prices missing" else "",
                        style = MaterialTheme.typography.bodySmall, color = BrownHomePalette.muted)
                }
            }
            Text("These are past plans and estimates, not actual spending history.",
                style = MaterialTheme.typography.bodySmall, color = BrownHomePalette.muted)
        }
    }
}

/** Owned by the full screen, so scrolling a lazy list cannot discard a pending photo result. */
@Composable
internal fun ShoppingPhotoReviewOverlay(
    selected: ShoppingList?,
    requested: Boolean,
    onRequestConsumed: () -> Unit,
    busy: Boolean,
    saveError: String?,
    onSave: (ShoppingList, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var scanning by remember { mutableStateOf(false) }
    var reviewText by remember { mutableStateOf<String?>(null) }
    var scanOwner by remember { mutableStateOf<String?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null && selected != null) {
            scanning = true
            scanError = null
            scanOwner = selected.id
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { text ->
                        if (scanOwner == selected.id) reviewText = text.text.take(24_000)
                        scanning = false
                    }
                    .addOnFailureListener {
                        if (scanOwner == selected.id) {
                            scanError = "Could not read the photo. Try a clearer image."
                        }
                        scanning = false
                    }
                    .addOnCompleteListener { recognizer.close() }
            } catch (_: Exception) {
                scanning = false
                scanError = "Could not open that photo. Please choose another image."
            }
        }
    }
    LaunchedEffect(requested) {
        if (requested) {
            onRequestConsumed()
            if (selected != null && !scanning) {
                scanError = null
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }
    LaunchedEffect(selected?.id) {
        if (selected?.id != scanOwner) {
            reviewText = null
            scanError = null
            scanOwner = null
        }
    }
    if (scanning && selected != null) {
        AlertDialog(onDismissRequest = {}, title = { Text("Reading photo locally") },
            text = { Text("Please wait while PesaTrack reads the image on your phone.") },
            confirmButton = {})
    }
    if (scanError != null && selected != null) {
        AlertDialog(onDismissRequest = { scanError = null }, title = { Text("Photo scan") },
            text = { Text(scanError.orEmpty()) },
            confirmButton = { TextButton(onClick = { scanError = null }) { Text("OK") } })
    }
    if (reviewText != null && selected?.id == scanOwner) {
        ShoppingPhotoTextReview(selected, reviewText.orEmpty(), busy, saveError,
            onDismiss = { reviewText = null }, onSave = { candidates ->
                val result = ShoppingIntelligencePolicy.importCandidates(selected, candidates) {
                    UUID.randomUUID().toString()
                }
                if (result.addedCount > 0) onSave(result.updated) { reviewText = null }
            })
    }
}

@Composable
private fun ShoppingPhotoTextReview(
    selected: ShoppingList,
    text: String,
    busy: Boolean,
    saveError: String?,
    onDismiss: () -> Unit,
    onSave: (List<ShoppingScanCandidate>) -> Unit
) {
    var edited by remember(selected.id, text) { mutableStateOf(text) }
    val candidates = remember(edited) { ShoppingIntelligencePolicy.parseLines(edited) }
    val preview = remember(selected, candidates) {
        var nextId = 0
        ShoppingIntelligencePolicy.importCandidates(selected, candidates) { "review-${nextId++}" }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Review scanned shopping items") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Edit the recognized lines before saving. Format: 2 x Milk KSh 120 or Bread @ 60.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = edited, onValueChange = { edited = it.take(24_000) },
                label = { Text("Recognized text (editable)") }, minLines = 5, maxLines = 9,
                modifier = Modifier.fillMaxWidth())
            Text("${preview.addedCount} to add · ${preview.skippedDuplicateCount} duplicates · " +
                "${preview.skippedCapacityCount} over list limit",
                style = MaterialTheme.typography.bodySmall)
            candidates.take(6).forEach { item ->
                Text("${item.quantity} × ${item.name} · " +
                    (item.unitPriceMinor?.let(::shoppingHintMoney) ?: "price unknown"),
                    style = MaterialTheme.typography.bodySmall)
            }
            if (candidates.size > 6) Text("+ ${candidates.size - 6} more recognized lines",
                style = MaterialTheme.typography.bodySmall)
            Text("All prices are unverified unit estimates. Nothing enters Activity or balances.",
                style = MaterialTheme.typography.bodySmall, color = BrownHomePalette.muted)
            if (saveError != null) Text(saveError, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        } }, confirmButton = {
            TextButton(onClick = { onSave(candidates) },
                enabled = !busy && preview.addedCount > 0) { Text("Add reviewed items") }
        }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

private fun shoppingHintMoney(minor: Long): String {
    val format = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    return "KSh ${format.format(BigDecimal.valueOf(minor, 2))}"
}

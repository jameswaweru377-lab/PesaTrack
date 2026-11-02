package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import com.kedevjimmy.pesatrack.ui.theme.ProfileVisualColors as C
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.rounded.Close
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PersonalDetailsScreen(padding: PaddingValues, onBack: () -> Unit, title: String = "Personal details") {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("pesatrack_preferences", 0) }
    var name by rememberSaveable { mutableStateOf(prefs.getString("profile_name", "").orEmpty()) }
    var avatar by rememberSaveable { mutableStateOf(prefs.getString("profile_avatar", "🌿") ?: "🌿") }
    var city by rememberSaveable { mutableStateOf(prefs.getString("profile_city", "").orEmpty()) }
    var bio by rememberSaveable { mutableStateOf(prefs.getString("profile_bio", "").orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showEmojiChoices by rememberSaveable { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize()) {
        ProfilePremiumPage(title, padding, onBack) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfilePhotoPicker(avatar = avatar, onPhotoSaved = {
                        scope.launch { snackbar.showSnackbar("Photo updated") }
                    })
                }
            }
            item {
                PesaCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Display name", style = MaterialTheme.typography.labelLarge,
                            color = C.muted)
                        OutlinedTextField(value = name,
                            onValueChange = { name = it.take(60); error = null; saved = false },
                            placeholder = { Text("What should we call you?") },
                            singleLine = true, shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(), isError = error == "Enter your display name.")
                        HorizontalDivider(color = C.outline)
                        Text("City or town (optional)", style = MaterialTheme.typography.labelLarge,
                            color = C.muted)
                        OutlinedTextField(value = city,
                            onValueChange = { city = it.take(60); error = null; saved = false },
                            placeholder = { Text("e.g. Nairobi") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                        Text("Shown only in your private profile. No location permission is needed.",
                            style = MaterialTheme.typography.bodySmall, color = C.muted)
                        HorizontalDivider(color = C.outline)
                        Text("About me (optional)", style = MaterialTheme.typography.labelLarge,
                            color = C.muted)
                        OutlinedTextField(value = bio,
                            onValueChange = { bio = it.take(160); error = null; saved = false },
                            placeholder = { Text("A little about your money journey") },
                            minLines = 2, maxLines = 3,
                            supportingText = { Text("${bio.length}/160") },
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item {
                TextButton(onClick = { showEmojiChoices = !showEmojiChoices }) {
                    Text(if (showEmojiChoices) "Hide avatar choices" else "Use an emoji instead")
                }
                if (showEmojiChoices) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()) {
                        listOf("🌿", "😊", "🧑🏾", "👩🏾").forEach { value ->
                            FilterChip(selected = avatar == value,
                                onClick = { avatar = value }, label = { Text(value) })
                        }
                    }
                }
            }
            item {
                ProfileHint("Saved on this phone. No sign-in required.")
            }
            item {
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium)
                }
                if (saved) Surface(color = C.greenSoft, shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = C.green)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Profile saved", color = C.ink, fontWeight = FontWeight.Bold)
                            Text("Your details are up to date", color = C.muted)
                        }
                        IconButton(onClick = { saved = false }) {
                            Icon(Icons.Rounded.Close, "Dismiss confirmation", tint = C.muted)
                        }
                    }
                }
            }
            item {
                ProfileGradientButton("Save changes", action = {
                    when (attemptProfileSave(
                        name, avatar, city, bio,
                        persist = { savedName, savedAvatar, savedCity, savedBio ->
                            prefs.edit().putString("profile_name", savedName)
                                .putString("profile_avatar", savedAvatar)
                                .putString("profile_city", savedCity)
                                .putString("profile_bio", savedBio).commit()
                        }
                    )) {
                        ProfileSaveResult.SAVED -> {
                            error = null
                            saved = true
                            scope.launch { delay(4000); saved = false }
                        }
                        ProfileSaveResult.INVALID_NAME ->
                            error = "Enter your display name."
                        ProfileSaveResult.FAILED ->
                            error = "Couldn't save changes. Try again."
                    }
                })
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter)
            .padding(padding).padding(horizontal = 16.dp))
    }
}

package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
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
    var email by rememberSaveable { mutableStateOf(prefs.getString("profile_email", "").orEmpty()) }
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
                            modifier = Modifier.fillMaxWidth())
                        HorizontalDivider(color = C.outline)
                        Text("Email (optional)", style = MaterialTheme.typography.labelLarge,
                            color = C.muted)
                        OutlinedTextField(value = email,
                            onValueChange = { email = it.take(120); error = null; saved = false },
                            placeholder = { Text("Add an email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
                        name, avatar, email,
                        emailValid = { android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches() },
                        persist = { savedName, savedAvatar, savedEmail ->
                            prefs.edit().putString("profile_name", savedName)
                                .putString("profile_avatar", savedAvatar)
                                .putString("profile_email", savedEmail).commit()
                        }
                    )) {
                        ProfileSaveResult.SAVED -> {
                            error = null
                            saved = true
                            scope.launch { delay(4000); saved = false }
                        }
                        ProfileSaveResult.INVALID_EMAIL ->
                            error = "Enter a valid email, or leave it blank."
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

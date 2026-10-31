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
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize()) {
        EverydayPage(title, padding, onBack) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Make it yours", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text("Your details are kept on this phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                PesaCard {
                    Column(Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        ProfilePhotoPicker(avatar = avatar, onPhotoSaved = {
                            scope.launch { snackbar.showSnackbar("Photo updated") }
                        })
                    }
                }
            }
            item {
                PesaCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Display name", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(value = name,
                            onValueChange = { name = it.take(60); error = null },
                            placeholder = { Text("What should we call you?") },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item {
                PesaCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Email (optional)", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(value = email,
                            onValueChange = { email = it.take(120); error = null },
                            placeholder = { Text("Add an email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth())
                        Text("Just for your records. No emails or sign-in needed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                PesaCard {
                    Row(Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text("Saved on this phone. No account needed.",
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium)
                }
                Button(onClick = {
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
                            scope.launch { snackbar.showSnackbar("Profile saved") }
                        }
                        ProfileSaveResult.INVALID_EMAIL ->
                            error = "Enter a valid email, or leave it blank."
                        ProfileSaveResult.FAILED ->
                            error = "Couldn't save changes. Try again."
                    }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                    Text("Save changes", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter)
            .padding(padding).padding(horizontal = 16.dp))
    }
}

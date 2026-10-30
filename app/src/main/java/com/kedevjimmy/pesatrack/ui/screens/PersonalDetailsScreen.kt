package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.PesaCard

@Composable
fun PersonalDetailsScreen(padding: PaddingValues, onBack: () -> Unit, title: String = "Personal details") {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("pesatrack_preferences", 0) }
    var name by rememberSaveable { mutableStateOf(prefs.getString("profile_name", "").orEmpty()) }
    var avatar by rememberSaveable { mutableStateOf(prefs.getString("profile_avatar", "🌿") ?: "🌿") }
    var email by rememberSaveable { mutableStateOf(prefs.getString("profile_email", "").orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var photoNotice by remember { mutableStateOf<String?>(null) }
    BackHandler(onBack = onBack)
    EverydayPage(title, padding, onBack) {
        item { ProfilePhotoPicker(avatar = avatar, onPhotoSaved = { photoNotice = "Photo updated on this phone." }) }
        item {
            PesaCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Display name", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = name, onValueChange = { name = it.take(60) },
                        label = { Text("Your name") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item {
            PesaCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = email, onValueChange = { email = it.take(120) },
                        label = { Text("Email (optional)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("For your reference only. We won't send you emails.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Emoji fallback", style = MaterialTheme.typography.titleSmall)
                Text("Shown when you don't have a profile photo.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("🌿", "😊", "🧑🏾", "👩🏾").forEach { value ->
                        FilterChip(selected = avatar == value, onClick = { avatar = value }, label = { Text(value) })
                    }
                }
            }
        }
        item {
            Text("Saved on this phone. No account sign-in required.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            photoNotice?.let { Text(it, color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                    error = "Enter a valid email or leave it blank."
                } else if (prefs.edit().putString("profile_name", name.trim())
                        .putString("profile_avatar", avatar).putString("profile_email", email.trim()).commit()
                ) onBack()
                else error = "Couldn't save your details. Please try again."
            }, modifier = Modifier.fillMaxWidth()) { Text("Save details") }
        }
    }
}

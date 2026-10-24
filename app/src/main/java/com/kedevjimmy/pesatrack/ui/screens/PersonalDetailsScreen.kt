package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    BackHandler(onBack = onBack)
    EverydayPage(title, padding, onBack) {
        item { Text("Make PesaTrack yours. These details stay on this phone and are not a sign-in account.") }
        item { OutlinedTextField(name, { name = it.take(60) }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(email, { email = it.take(120) }, label = { Text("Email (optional)") }, supportingText = { Text("For your own reference. We won't send you email.") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { Text("Choose an avatar", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("🌿", "😊", "🧑🏾", "👩🏾").forEach { value ->
                FilterChip(selected = avatar == value, onClick = { avatar = value }, label = { Text(value) })
            } }
        }
        item { error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) error = "Enter a valid email or leave it blank."
                else if (prefs.edit().putString("profile_name", name.trim()).putString("profile_avatar", avatar).putString("profile_email", email.trim()).commit()) onBack()
                else error = "Couldn't save your details. Please try again."
            }, modifier = Modifier.fillMaxWidth()) { Text("Save details") }
        }
    }
}

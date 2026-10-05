package com.kedevjimmy.pesatrack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConceptLoadError(message: String?, retry: () -> Unit) {
    if (message != null) Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(message)
            TextButton(onClick = retry) { Text("Try again") }
        }
    }
}

package com.kedevjimmy.pesatrack.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.kedevjimmy.pesatrack.ui.theme.ProfileVisualColors as C
import com.kedevjimmy.pesatrack.data.profile.ProfilePhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A permission-free Android picker and a local crop preview for a private profile photo. */
@Composable
internal fun ProfilePhotoPicker(avatar: String, onPhotoSaved: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { ProfilePhotoStore(context) }
    val scope = rememberCoroutineScope()
    var savedPhoto by remember { mutableStateOf(store.load()) }
    var candidate by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            busy = true
            error = null
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) { store.decodeSelected(uri) }
                candidate = bitmap
                if (bitmap == null) error = "This photo couldn't be opened. Choose another image under 12 MB."
                busy = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(156.dp), contentAlignment = Alignment.Center) {
          Box(Modifier.size(148.dp).clip(CircleShape).background(C.lilac),
            contentAlignment = Alignment.Center) {
            val photo = savedPhoto
            if (photo != null) {
                Image(photo.asImageBitmap(), "Your profile photo", Modifier.size(148.dp), contentScale = ContentScale.Crop)
            } else {
                Text(avatar, style = MaterialTheme.typography.headlineLarge)
            }
          }
          Box(Modifier.align(Alignment.BottomEnd).size(40.dp)
              .clip(CircleShape).background(C.violet), contentAlignment = Alignment.Center) {
              Icon(Icons.Rounded.AddAPhoto, "Change profile photo", tint = Color.White,
                  modifier = Modifier.size(24.dp))
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }, enabled = !busy, shape = RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = C.lilac, contentColor = C.violet)) {
                Icon(Icons.Rounded.AddAPhoto, null, modifier = Modifier.size(18.dp))
                Text(if (savedPhoto == null) "Choose photo" else "Change photo", Modifier.padding(start = 7.dp))
            }
            if (savedPhoto != null) {
                OutlinedButton(onClick = {
                    busy = true
                    scope.launch {
                        val removed = withContext(Dispatchers.IO) { store.clear() }
                        if (removed) {
                            savedPhoto = null
                            onPhotoSaved()
                        } else error = "Couldn't remove this photo. Try again."
                        busy = false
                    }
                }, enabled = !busy, shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = C.red)) {
                    Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(18.dp))
                    Text("Remove", Modifier.padding(start = 6.dp))
                }
            }
        }
        Text("Choose and crop a photo from your device", style = MaterialTheme.typography.bodySmall,
            color = C.muted)
        if (busy) CircularProgressIndicator(Modifier.size(24.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }

    candidate?.let { image ->
        ProfilePhotoCropDialog(
            bitmap = image,
            dismiss = { candidate = null },
            save = { crop ->
                candidate = null
                busy = true
                scope.launch {
                    val success = withContext(Dispatchers.IO) { store.save(crop) }
                    if (success) {
                        savedPhoto = store.load()
                        error = null
                        onPhotoSaved()
                    } else error = "Couldn't save this photo. Your previous photo is unchanged."
                    busy = false
                }
            }
        )
    }
}

@Composable
private fun ProfilePhotoCropDialog(bitmap: Bitmap, dismiss: () -> Unit, save: (Bitmap) -> Unit) {
    var zoom by remember(bitmap) { mutableFloatStateOf(1f) }
    var panX by remember(bitmap) { mutableFloatStateOf(0f) }
    var panY by remember(bitmap) { mutableFloatStateOf(0f) }
    var viewport by remember { mutableFloatStateOf(1f) }
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val crop = ProfilePhotoCropPolicy.cropRect(bitmap.width, bitmap.height, zoom, panX, panY)

    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Crop profile photo") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(
                    Modifier.size(250.dp).clip(CircleShape)
                        .onSizeChanged { viewport = it.width.toFloat().coerceAtLeast(1f) }
                        .pointerInput(bitmap) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                panX = (panX - 2f * drag.x / viewport).coerceIn(-1f, 1f)
                                panY = (panY - 2f * drag.y / viewport).coerceIn(-1f, 1f)
                            }
                        }
                        .semantics { contentDescription = "Profile photo crop preview. Drag to position the photo." }
                ) {
                    drawImage(
                        image = image,
                        srcOffset = IntOffset(crop.left, crop.top),
                        srcSize = IntSize(crop.size, crop.size),
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        filterQuality = FilterQuality.High
                    )
                }
                Text("Zoom", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
                Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 1f..4f)
                Text("Drag to position · Pinch-free zoom slider", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                save(Bitmap.createBitmap(bitmap, crop.left, crop.top, crop.size, crop.size))
            }) { Text("Use photo") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } }
    )
}

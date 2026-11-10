package com.kedevjimmy.pesatrack.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.R
import com.kedevjimmy.pesatrack.data.profile.ProfileCoverStore
import com.kedevjimmy.pesatrack.data.profile.ProfilePhotoStore
import com.kedevjimmy.pesatrack.ui.theme.ProfileVisualColors as C
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ProfileDetailTopBar(back: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = back) {
            Icon(Icons.Rounded.ChevronLeft, "Back to More", tint = C.ink)
        }
        Text("Profile", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = C.ink)
    }
}

@Composable
internal fun ProfileCoverHeader(
    name: String,
    avatar: String,
    onBack: () -> Unit,
    onAvatarEdit: () -> Unit
) {
    val context = LocalContext.current
    val coverStore = remember(context) { ProfileCoverStore(context) }
    val photoStore = remember(context) { ProfilePhotoStore(context) }
    val avatarPhoto = remember(context) { photoStore.load() }
    var coverPhoto by remember(context) { mutableStateOf(coverStore.load()) }
    var coverMenu by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val success = withContext(Dispatchers.IO) {
                val decoded = photoStore.decodeSelected(uri)
                try { decoded != null && coverStore.save(decoded) }
                finally { decoded?.recycle() }
            }
            if (success) coverPhoto = withContext(Dispatchers.IO) { coverStore.load() }
            busy = false
            snackbar.showSnackbar(if (success) "Cover photo updated" else "Couldn't save this image. Try another photo under 12 MB.")
        }
    }
    val chooseCover = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    if (coverMenu) AlertDialog(
        onDismissRequest = { coverMenu = false },
        title = { Text("Cover photo") },
        text = { Text("Choose an image from this phone or restore the default Nairobi cover.") },
        confirmButton = { TextButton(onClick = { coverMenu = false; chooseCover() }) { Text("Choose photo") } },
        dismissButton = { TextButton(onClick = {
            coverMenu = false
            scope.launch {
                val success = withContext(Dispatchers.IO) { coverStore.clear() }
                if (success) coverPhoto = null
                snackbar.showSnackbar(if (success) "Default cover restored" else "Couldn't remove the cover")
            }
        }) { Text("Restore default") } }
    )

    Box(Modifier.fillMaxWidth().height(272.dp)) {
        ProfileCoverArtwork(coverPhoto, busy, onBack, onEdit = {
            if (coverPhoto == null) chooseCover() else coverMenu = true
        })
        ProfileAvatarArtwork(name, avatar, avatarPhoto, onAvatarEdit,
            modifier = Modifier.align(Alignment.BottomCenter))
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ProfileCoverArtwork(
    cover: Bitmap?, busy: Boolean, onBack: () -> Unit, onEdit: () -> Unit
) {
    Box(Modifier.fillMaxWidth().height(216.dp).background(C.sand)) {
        if (cover == null) {
            Image(painterResource(R.drawable.profile_default_nairobi_cover),
                "Default Nairobi cover photo", Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop)
        } else {
            Image(cover.asImageBitmap(), "Your cover photo", Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop)
        }
        IconButton(onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp).size(48.dp)
                .clip(CircleShape).background(Color.Black.copy(alpha = .32f))) {
            Icon(Icons.Rounded.ChevronLeft, "Back", Modifier.size(32.dp), tint = Color.White)
        }
        IconButton(onClick = onEdit, enabled = !busy,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(48.dp)
                .clip(CircleShape).background(Color.Black.copy(alpha = .32f))) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White,
                strokeWidth = 2.dp)
            else Icon(Icons.Rounded.Edit, "Change cover photo", Modifier.size(20.dp), tint = Color.White)
        }
    }
}

@Composable
private fun ProfileAvatarArtwork(
    name: String, avatar: String, avatarPhoto: Bitmap?, onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.size(112.dp)) {
        Surface(Modifier.fillMaxSize(), shape = CircleShape,
            color = C.cream, border = BorderStroke(4.dp, C.cream)) {
            Box(contentAlignment = Alignment.Center) {
                if (avatarPhoto != null) Image(avatarPhoto.asImageBitmap(), "Profile photo",
                    Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Text(profileAvatarLabel(name, avatar), color = C.coffee,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                    fontSize = 36.sp)
            }
        }
        IconButton(onClick = onEdit,
            modifier = Modifier.align(Alignment.BottomEnd).size(48.dp)
                .clip(CircleShape).background(C.ink)) {
            Icon(Icons.Rounded.PhotoCamera, "Change profile photo", Modifier.size(22.dp), tint = Color.White)
        }
    }
}

@Composable
internal fun ProfileIdentityDetails(name: String, city: String, bio: String, onEdit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(name.ifBlank { "Your profile" }, style = MaterialTheme.typography.headlineSmall,
            color = C.ink, fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text("PERSONAL • PRIVATE • ON DEVICE", color = C.coffee,
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        if (city.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocationOn, null, Modifier.size(16.dp), tint = C.muted)
            Text(city, color = C.muted, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 4.dp))
        }
        if (bio.isNotBlank()) Text(bio, color = C.muted,
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        TextButton(onClick = onEdit, colors = ButtonDefaults.textButtonColors(contentColor = C.coffee)) {
            Icon(Icons.Rounded.Edit, null, Modifier.size(16.dp))
            Text("Edit profile", Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
        }
    }
}

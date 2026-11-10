package com.kedevjimmy.pesatrack.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

    Box(Modifier.fillMaxWidth().background(C.cream)) {
        ProfileCoverArtwork(coverPhoto, busy, onBack, onEdit = {
            if (coverPhoto == null) chooseCover() else coverMenu = true
        })
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(168.dp)) {
                Column(
                    Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Pesa\nTrack", color = C.coffee, fontFamily = FontFamily.Cursive,
                        fontSize = 26.sp, lineHeight = 24.sp, textAlign = TextAlign.Center)
                    Box(Modifier.padding(top = 4.dp).width(42.dp).height(2.dp)
                        .background(C.leaf, CircleShape))
                }
                ProfileAvatarArtwork(name, avatar, avatarPhoto, onAvatarEdit,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 68.dp))
            }
            ProfileIdentityDetails(name)
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ProfileCoverArtwork(
    cover: Bitmap?, busy: Boolean, onBack: () -> Unit, onEdit: () -> Unit
) {
    Box(Modifier.fillMaxWidth().height(264.dp).background(C.sand)) {
        if (cover == null) {
            Image(painterResource(R.drawable.profile_default_nairobi_cover),
                "Default Nairobi cover photo", Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop)
        } else {
            Image(cover.asImageBitmap(), "Your cover photo", Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop)
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color.Transparent,
            .28f to Color.Transparent,
            .49f to C.cream.copy(alpha = .36f),
            .72f to C.cream.copy(alpha = .90f),
            .95f to C.cream,
            1f to C.cream
        )))
        IconButton(onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp).size(48.dp)
                .clip(CircleShape).background(Color.Black.copy(alpha = .32f))) {
            Icon(Icons.Rounded.ChevronLeft, "Back", Modifier.size(32.dp), tint = Color.White)
        }
        IconButton(onClick = onEdit, enabled = !busy,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(48.dp)
                .clip(CircleShape).background(Color.Black.copy(alpha = .32f))) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White,
                strokeWidth = 2.dp)
            else Icon(Icons.Rounded.MoreHoriz, "Change cover photo", Modifier.size(24.dp), tint = Color.White)
        }
    }
}

@Composable
private fun ProfileAvatarArtwork(
    name: String, avatar: String, avatarPhoto: Bitmap?, onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.size(96.dp).clip(CircleShape)
        .clickable(onClick = onEdit, role = Role.Button)
        .semantics { contentDescription = "Change profile photo"; role = Role.Button }) {
        Surface(Modifier.fillMaxSize(), shape = CircleShape,
            color = C.cream, border = BorderStroke(4.dp, C.cream)) {
            Box(contentAlignment = Alignment.Center) {
                if (avatarPhoto != null) Image(avatarPhoto.asImageBitmap(), "Profile photo",
                    Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Text(profileAvatarLabel(name, avatar), color = C.coffee,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                    fontSize = 32.sp)
            }
        }
    }
}

@Composable
internal fun ProfileIdentityDetails(name: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name.ifBlank { "Your profile" }, modifier = Modifier.widthIn(max = 248.dp),
                fontSize = 24.sp,
                color = C.ink, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Rounded.CheckCircle, "Profile stored on device",
                modifier = Modifier.size(18.dp), tint = C.leaf)
        }
        Text("PERSONAL • PRIVATE • ON DEVICE", color = C.coffee,
            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text("Your money, your plans", color = C.ink, fontSize = 14.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        Box(Modifier.width(40.dp).height(2.dp).background(C.leaf, CircleShape))
    }
}

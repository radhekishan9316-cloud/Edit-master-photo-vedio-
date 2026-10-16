package com.editmaster.app

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.VideoView
import android.widget.MediaController
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF0A1020)
private val Blue = Color(0xFF3978FF)
private val Purple = Color(0xFF8757FF)
private val Orange = Color(0xFFFF7A24)
private val CanvasBg = Color(0xFFF4F6FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EditMasterApp() }
    }
}

private enum class Screen { HOME, PHOTO, VIDEO, CREATE, PROJECTS, DISCOVER, PROFILE }

@Composable
private fun EditMasterApp() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    var selectedVideo by remember { mutableStateOf<Uri?>(null) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var savedCount by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    selectedImage = BitmapFactory.decodeStream(input)
                }
                if (selectedImage != null) screen = Screen.PHOTO
            } catch (_: Exception) {
                Toast.makeText(context, "Could not open this image. Try another file.", Toast.LENGTH_LONG).show()
            }
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { selectedVideo = uri; screen = Screen.VIDEO }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, secondary = Purple, background = CanvasBg)) {
        Scaffold(
            containerColor = CanvasBg,
            bottomBar = {
                if (screen !in listOf(Screen.PHOTO, Screen.VIDEO)) {
                    NavigationBar(containerColor = Color.White) {
                        NavItem("Home", Icons.Default.Home, screen == Screen.HOME) { screen = Screen.HOME }
                        NavItem("Discover", Icons.Default.Explore, screen == Screen.DISCOVER) { screen = Screen.DISCOVER }
                        NavItem("Create", Icons.Default.AddCircle, screen == Screen.CREATE) { screen = Screen.CREATE }
                        NavItem("Projects", Icons.Default.Folder, screen == Screen.PROJECTS) { screen = Screen.PROJECTS }
                        NavItem("Profile", Icons.Default.Person, screen == Screen.PROFILE) { screen = Screen.PROFILE }
                    }
                }
            }
        ) { padding ->
            when (screen) {
                Screen.HOME -> HomeScreen(Modifier.padding(padding),
                    onPhoto = { photoPicker.launch("image/*") }, onVideo = { videoPicker.launch("video/*") },
                    onCreate = { screen = Screen.CREATE }, onDiscover = { screen = Screen.DISCOVER })
                Screen.PHOTO -> PhotoEditorScreen(
                    bitmap = selectedImage, brightness = brightness, saturation = saturation,
                    onBrightness = { brightness = it }, onSaturation = { saturation = it },
                    onBack = { screen = Screen.HOME }, onSave = {
                        val original = selectedImage
                        if (original != null) {
                            try {
                                val matrix = ColorMatrix().apply {
                                    setSaturation(saturation)
                                    val b = brightness * 255f
                                    postConcat(ColorMatrix(floatArrayOf(
                                        1f,0f,0f,0f,b, 0f,1f,0f,0f,b, 0f,0f,1f,0f,b, 0f,0f,0f,1f,0f
                                    )))
                                }
                                val out = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
                                Canvas(out).drawBitmap(original, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) })
                                val values = ContentValues().apply {
                                    put(MediaStore.Images.Media.DISPLAY_NAME, "EditMaster_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg")
                                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                    if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EditMaster")
                                }
                                val outputUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                                if (outputUri != null) context.contentResolver.openOutputStream(outputUri)?.use { out.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                                savedCount++
                                Toast.makeText(context, "Edited photo saved to Pictures/EditMaster", Toast.LENGTH_LONG).show()
                            } catch (_: Exception) {
                                Toast.makeText(context, "Save failed. Check available storage and try again.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
                Screen.VIDEO -> VideoPreviewScreen(selectedVideo, onBack = { screen = Screen.HOME })
                Screen.CREATE -> CreateScreen(onPhoto = { photoPicker.launch("image/*") }, onVideo = { videoPicker.launch("video/*") }, onOther = { Toast.makeText(context, "This tool is planned for the next development stage.", Toast.LENGTH_SHORT).show() }, modifier = Modifier.padding(padding))
                Screen.PROJECTS -> SimpleScreen("My Projects", "Saved exports appear in your device gallery under Pictures/EditMaster.", Icons.Default.Folder, Modifier.padding(padding))
                Screen.DISCOVER -> SimpleScreen("Discover", "Curated starter tools and editing shortcuts. Online templates will be connected after a content backend is configured.", Icons.Default.Explore, Modifier.padding(padding))
                Screen.PROFILE -> SimpleScreen("Your Studio", "Edit Master · Version 0.1.0\nBasic editing works without an account.", Icons.Default.Person, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(selected = selected, onClick = onClick, icon = { Icon(icon, contentDescription = label) }, label = { Text(label, fontSize = 10.sp) })
}

@Composable
private fun HomeScreen(modifier: Modifier, onPhoto: () -> Unit, onVideo: () -> Unit, onCreate: () -> Unit, onDiscover: () -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(BrushGradient()), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(27.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("EDIT MASTER", color = Navy, fontWeight = FontWeight.Black, fontSize = 19.sp, letterSpacing = 1.sp)
                Text("Create More. Edit Better.", color = Color.Gray, fontSize = 11.sp)
            }
            IconButton(onClick = onDiscover) { Icon(Icons.Default.Search, "Search", tint = Navy) }
            IconButton(onClick = onDiscover) { Icon(Icons.Default.NotificationsNone, "Notifications", tint = Navy) }
        }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(BrushGradient()).padding(22.dp)) {
            Column {
                Text("WELCOME TO", color = Color.White.copy(alpha = .75f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Your Creative\nStudio.", color = Color.White, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Text("Edit photos, shape videos and create content — all in one place.", color = Color.White.copy(alpha = .88f), fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(onClick = onPhoto, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Navy)) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(5.dp)); Text("Edit Photo") }
                    OutlinedButton(onClick = onVideo, border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .7f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Icon(Icons.Default.PlayCircle, null); Spacer(Modifier.width(5.dp)); Text("Edit Video") }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        SectionTitle("Create something amazing", "Choose a tool")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolCard("Reels Maker", "Short-form video", Icons.Default.Movie, Purple, Modifier.weight(1f), onVideo)
            ToolCard("AI Studio", "Creative AI tools", Icons.Default.AutoFixHigh, Orange, Modifier.weight(1f), onCreate)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolCard("Thumbnail", "YouTube & social", Icons.Default.Collections, Blue, Modifier.weight(1f), onCreate)
            ToolCard("Audio Studio", "Sound & voice", Icons.Default.GraphicEq, Color(0xFF20A88A), Modifier.weight(1f), onCreate)
        }
        Spacer(Modifier.height(24.dp))
        SectionTitle("TODAY'S HIGHLIGHTS", "Explore ideas")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HighlightCard("Quick Editing Tips", "Learn a new trick", "✦", Purple, onDiscover)
            HighlightCard("Festival Studio", "Design for occasions", "✺", Orange, onCreate)
            HighlightCard("Thumbnail Ideas", "Make it stand out", "▶", Blue, onCreate)
        }
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Ready when you are", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy); Text("Start with a photo or video on your device.", fontSize = 12.sp, color = Color.Gray) }
            Icon(Icons.Default.ArrowForward, null, tint = Blue)
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun BrushGradient(): androidx.compose.ui.graphics.Brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Navy, Blue, Purple))

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(subtitle, color = Blue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ToolCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(tint.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(23.dp)) }
            Spacer(Modifier.height(12.dp)); Text(title, color = Navy, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(subtitle, color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun HighlightCard(title: String, subtitle: String, symbol: String, tint: Color, onClick: () -> Unit) {
    Card(Modifier.width(220.dp).height(124.dp).clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxSize().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, color = Navy, fontWeight = FontWeight.Bold, fontSize = 14.sp); Spacer(Modifier.height(5.dp)); Text(subtitle, color = Color.Gray, fontSize = 11.sp); Spacer(Modifier.height(10.dp)); Text("EXPLORE  →", color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(tint.copy(alpha = .13f)), contentAlignment = Alignment.Center) { Text(symbol, color = tint, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun PhotoEditorScreen(bitmap: Bitmap?, brightness: Float, saturation: Float, onBrightness: (Float) -> Unit, onSaturation: (Float) -> Unit, onBack: () -> Unit, onSave: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Navy).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Text("Photo Editor", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(5.dp)); Text("Save") }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF151D30)), contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                val previewMatrix = remember(brightness, saturation) {
                    ColorMatrix().apply { setSaturation(saturation); val b = brightness * 255f; postConcat(ColorMatrix(floatArrayOf(1f,0f,0f,0f,b, 0f,1f,0f,0f,b, 0f,0f,1f,0f,b, 0f,0f,0f,1f,0f))) }
                }
                val matrixValues = FloatArray(20)
                previewMatrix.getArray(matrixValues)
                Image(bitmap.asImageBitmap(), contentDescription = "Photo preview", modifier = Modifier.fillMaxWidth().padding(6.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(matrixValues)))
            } else Text("Choose a photo to begin", color = Color.White)
        }
        Text("Brightness", color = Color.White, fontWeight = FontWeight.SemiBold)
        Slider(value = brightness, onValueChange = onBrightness, valueRange = -0.5f..0.5f, colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue))
        Text("Saturation", color = Color.White, fontWeight = FontWeight.SemiBold)
        Slider(value = saturation, onValueChange = onSaturation, valueRange = 0f..2f, colors = SliderDefaults.colors(thumbColor = Purple, activeTrackColor = Purple))
        Text("Basic adjustments · export saves a new JPEG copy", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun VideoPreviewScreen(uri: Uri?, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Navy).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Text("Video Studio", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        if (uri != null) {
            AndroidView(factory = { ctx -> VideoView(ctx).apply { setVideoURI(uri); setMediaController(MediaController(ctx).also { it.setAnchorView(this) }); setOnPreparedListener { it.isLooping = false; start() } } }, modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)))
        } else Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Choose a video to preview", color = Color.White) }
        Spacer(Modifier.height(12.dp))
        Text("Video import and playback preview are enabled. Trim, timeline editing and export are not yet implemented in this starter build.", color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to Studio") }
    }
}

@Composable
private fun CreateScreen(onPhoto: () -> Unit, onVideo: () -> Unit, onOther: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Text("Create", fontSize = 28.sp, color = Navy, fontWeight = FontWeight.ExtraBold)
        Text("Pick a workflow to get started", color = Color.Gray)
        Spacer(Modifier.height(18.dp))
        ToolCard("Photo Editor", "Adjust brightness and saturation", Icons.Default.Image, Blue, Modifier.fillMaxWidth(), onPhoto)
        Spacer(Modifier.height(10.dp)); ToolCard("Video Studio", "Import and preview a video", Icons.Default.Movie, Purple, Modifier.fillMaxWidth(), onVideo)
        Spacer(Modifier.height(10.dp)); ToolCard("Reels Maker", "Vertical short-video workflow", Icons.Default.VideoLibrary, Orange, Modifier.fillMaxWidth(), onOther)
        Spacer(Modifier.height(10.dp)); ToolCard("Thumbnail & Poster", "Design tools — next stage", Icons.Default.Collections, Blue, Modifier.fillMaxWidth(), onOther)
        Spacer(Modifier.height(10.dp)); ToolCard("AI Magic Studio", "Requires a configured AI service", Icons.Default.AutoFixHigh, Purple, Modifier.fillMaxWidth(), onOther)
        Spacer(Modifier.height(10.dp)); ToolCard("Audio Studio", "Audio editing — next stage", Icons.Default.GraphicEq, Orange, Modifier.fillMaxWidth(), onOther)
    }
}

@Composable
private fun SimpleScreen(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Blue, modifier = Modifier.size(54.dp)); Spacer(Modifier.height(16.dp)); Text(title, color = Navy, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp); Spacer(Modifier.height(8.dp)); Text(description, color = Color.Gray, fontSize = 14.sp, lineHeight = 21.sp)
    }
}

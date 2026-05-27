package org.mirai.app.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mirai.app.ui.viewmodel.MangaViewModel

val ThemeSeedColors = listOf(
    Color(0xFF4A6572), // Slate Blue
    Color(0xFF8D6E63), // Cocoa Bronze
    Color(0xFFE53935), // Crimson Red
    Color(0xFF1E88E5), // Vivid Blue
    Color(0xFF43A047), // Emerald Green
    Color(0xFF8E24AA)  // Purple Royal
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MangaViewModel, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val liveColorVal by viewModel.settingsManager.themeColor.collectAsStateWithLifecycle()
    val kcEnabled by viewModel.settingsManager.komikCastEnabled.collectAsStateWithLifecycle()
    val sgEnabled by viewModel.settingsManager.shinigamiEnabled.collectAsStateWithLifecycle()
    val readerMode by viewModel.settingsManager.readerMode.collectAsStateWithLifecycle()
    val tapToZoom by viewModel.settingsManager.tapToZoom.collectAsStateWithLifecycle()
    val cacheBytes by viewModel.settingsManager.cacheBytesUsed.collectAsStateWithLifecycle()

    Scaffold(
        // FIX 1: Matikan insets bawaan agar tidak membuat gap kosong di atas bottom bar
        contentWindowInsets = WindowInsets(0, 0, 0, 0), 
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0), 
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // FIX 2: Hanya ambil top padding dari Scaffold (untuk TopAppBar) agar bagian bottom mentok 0.dp
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 0.dp 
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp) 
        ) {
            SettingsHeader(title = "Display & Styling")

            Text(
                text = "Dynamic Color Palette Scheme",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(ThemeSeedColors) { seedColor ->
                    val isSelected = liveColorVal == seedColor.value.toInt()
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(seedColor)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                viewModel.settingsManager.setThemeColor(seedColor.value.toInt())
                                Toast.makeText(context, "Theme updated!", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsHeader(title = "Manga Sources")

            ListItem(
                headlineContent = { Text("KomikCast Indonesian Provider") },
                supportingContent = { Text("Fetch popular manga and search from KomikCast") },
                trailingContent = {
                    Switch(checked = kcEnabled, onCheckedChange = { viewModel.settingsManager.setKomikCastEnabled(it) })
                }
            )

            ListItem(
                headlineContent = { Text("Shinigami Indonesian Provider") },
                supportingContent = { Text("Fetch popular manga and search from Shinigami API") },
                trailingContent = {
                    Switch(checked = sgEnabled, onCheckedChange = { viewModel.settingsManager.setShinigamiEnabled(it) })
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            SettingsHeader(title = "Reader Settings")

            ListItem(
                headlineContent = { Text("Reading Orientation direction") },
                supportingContent = { Text(if (readerMode == "vertical") "Vertical Webtoon continuous" else "Horizontal single page flip") },
                trailingContent = {
                    Row {
                        FilterChip(
                            selected = readerMode == "vertical",
                            onClick = { viewModel.settingsManager.setReaderMode("vertical") },
                            label = { Text("Vertical") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = readerMode == "horizontal",
                            onClick = { viewModel.settingsManager.setReaderMode("horizontal") },
                            label = { Text("Horizontal") }
                        )
                    }
                }
            )

            ListItem(
                headlineContent = { Text("Tap to zoom page scale gesture") },
                supportingContent = { Text("Allows double pinch/pan zooming inside manga pages") },
                trailingContent = {
                    Switch(checked = tapToZoom, onCheckedChange = { viewModel.settingsManager.setTapToZoom(it) })
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            SettingsHeader(title = "Manga Store Cache")

            val cacheMbFormat = remember(cacheBytes) {
                String.format("%.2f MB", cacheBytes.toDouble() / (1024 * 1024))
            }

            ListItem(
                headlineContent = { Text("Manga Image Cache") },
                supportingContent = { Text("Temporary files used: $cacheMbFormat") },
                trailingContent = {
                    Button(
                        onClick = {
                            viewModel.settingsManager.clearCache()
                            Toast.makeText(context, "Manga cached pages cleared!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear Cache", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
package org.mirai.app.ui.screen

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mirai.app.ui.viewmodel.MangaViewModel

// Daftar warna seed yang bisa dipilih, tiap entry punya nama & warna
data class ThemeSeedEntry(val name: String, val color: Color)

val ThemeSeedColors = listOf(
    ThemeSeedEntry("Slate Blue",    Color(0xFF4A6572)),
    ThemeSeedEntry("Cocoa Bronze",  Color(0xFF8D6E63)),
    ThemeSeedEntry("Crimson Red",   Color(0xFFE53935)),
    ThemeSeedEntry("Vivid Blue",    Color(0xFF1E88E5)),
    ThemeSeedEntry("Emerald Green", Color(0xFF43A047)),
    ThemeSeedEntry("Purple Royal",  Color(0xFF8E24AA)),
    ThemeSeedEntry("Sunset Orange", Color(0xFFF4511E)),
    ThemeSeedEntry("Teal",          Color(0xFF00897B)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MangaViewModel, onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    val liveColorVal  by viewModel.settingsManager.themeColor.collectAsStateWithLifecycle()
    val isDynamicColor by viewModel.settingsManager.isDynamicColor.collectAsStateWithLifecycle()
    val isDarkTheme   by viewModel.settingsManager.isDarkTheme.collectAsStateWithLifecycle()
    val kcEnabled     by viewModel.settingsManager.komikCastEnabled.collectAsStateWithLifecycle()
    val sgEnabled     by viewModel.settingsManager.shinigamiEnabled.collectAsStateWithLifecycle()
    val readerMode    by viewModel.settingsManager.readerMode.collectAsStateWithLifecycle()
    val tapToZoom     by viewModel.settingsManager.tapToZoom.collectAsStateWithLifecycle()
    val cacheBytes    by viewModel.settingsManager.cacheBytesUsed.collectAsStateWithLifecycle()

    var showColorDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        TopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0), 
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding(), bottom = 0.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            // ── SECTION: Display ──────────────────────────────────
            SettingsHeader(title = "Tampilan")

            ListItem(
                headlineContent = { Text("Mode Gelap") },
                supportingContent = {
                    Text(if (isDarkTheme) "Mode gelap aktif" else "Mode gelap nonaktif")
                },
                leadingContent = {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { viewModel.settingsManager.setDarkTheme(it) }
                    )
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text("Material You (Dynamic Color)") },
                    supportingContent = {
                        Text(
                            if (isDynamicColor)
                                "Warna diambil dari wallpaper kamu secara otomatis"
                            else
                                "Matikan untuk memilih warna seed manual"
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isDynamicColor,
                            onCheckedChange = { viewModel.settingsManager.setDynamicColor(it) }
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            }

            ListItem(
                headlineContent = {
                    Text(
                        "Warna Aksen",
                        color = if (isDynamicColor)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                },
                supportingContent = {
                    val currentName = ThemeSeedColors
                        .find { it.color.value.toInt() == liveColorVal }?.name
                        ?: "Custom"
                    Text(
                        text = if (isDynamicColor) "Dinonaktifkan saat Material You aktif"
                               else "Warna sekarang: $currentName",
                        color = if (isDynamicColor)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(liveColorVal))
                    )
                },
                trailingContent = {
                    TextButton(
                        onClick = { if (!isDynamicColor) showColorDialog = true },
                        enabled = !isDynamicColor
                    ) {
                        Text("Pilih")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── SECTION: Manga Sources ──────────────────────────────────────
            SettingsHeader(title = "Provider Manga")

            ListItem(
                headlineContent = { Text("KomikCast") },
                supportingContent = { Text("Tampilkan komik dari KomikCast") },
                trailingContent = {
                    Switch(
                        checked = kcEnabled,
                        onCheckedChange = { viewModel.settingsManager.setKomikCastEnabled(it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("Shinigami") },
                supportingContent = { Text("Tampilkan komik dari Shinigami") },
                trailingContent = {
                    Switch(
                        checked = sgEnabled,
                        onCheckedChange = { viewModel.settingsManager.setShinigamiEnabled(it) }
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── SECTION: Reader ─────────────────────────────────────────────
            SettingsHeader(title = "Pengaturan Pembaca")

            ListItem(
                headlineContent = { Text("Mode Baca") },
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
                headlineContent = { Text("Double tap untuk zoom") },
                trailingContent = {
                    Switch(
                        checked = tapToZoom,
                        onCheckedChange = { viewModel.settingsManager.setTapToZoom(it) }
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── SECTION: Cache ──────────────────────────────────────────────
            SettingsHeader(title = "Cache")

            val cacheMbFormat = remember(cacheBytes) {
                String.format("%.2f MB", cacheBytes.toDouble() / (1024 * 1024))
            }

            ListItem(
                headlineContent = { Text("Cache Gambar") },
                supportingContent = { Text("Temporary files used: $cacheMbFormat") },
                trailingContent = {
                    Button(
                        onClick = {
                            viewModel.settingsManager.clearCache()
                            Toast.makeText(context, "Cache dibersihkan!!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Hapus Cache", color = Color.White)
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── DIALOG POPUP PILIH WARNA ────────────────────────────────────────────
    if (showColorDialog) {
        ColorPickerDialog(
            currentColorVal = liveColorVal,
            onColorSelected = { colorEntry ->
                viewModel.settingsManager.setThemeColor(colorEntry.color.value.toInt())
                Toast.makeText(context, "Tema '${colorEntry.name}' diterapkan!", Toast.LENGTH_SHORT).show()
                showColorDialog = false
            },
            onDismiss = { showColorDialog = false }
        )
    }
}

@Composable
fun ColorPickerDialog(
    currentColorVal: Int,
    onColorSelected: (ThemeSeedEntry) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Pilih Warna Aksen",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Pilih warna yang akan digunakan sebagai tema aplikasi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Grid 2 kolom
                val rows = ThemeSeedColors.chunked(2)
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { entry ->
                            val isSelected = entry.color.value.toInt() == currentColorVal
                            ColorOptionCard(
                                entry = entry,
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = { onColorSelected(entry) }
                            )
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Batal")
                }
            }
        }
    }
}

@Composable
fun ColorOptionCard(
    entry: ThemeSeedEntry,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected)
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp
            )
        else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(entry.color)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = entry.name,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
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
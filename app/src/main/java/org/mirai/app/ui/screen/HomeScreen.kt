package org.mirai.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LayersClear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import android.widget.Toast
import org.mirai.app.data.model.Manga
import org.mirai.app.ui.viewmodel.MangaViewModel
import org.mirai.app.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MangaViewModel,
    navController: NavController,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchState by viewModel.searchResult.collectAsStateWithLifecycle()

    val kcPopular by viewModel.komikCastPopular.collectAsStateWithLifecycle()
    val sgPopular by viewModel.shinigamiPopular.collectAsStateWithLifecycle()

    val kcEnabled by viewModel.settingsManager.komikCastEnabled.collectAsStateWithLifecycle()
    val sgEnabled by viewModel.settingsManager.shinigamiEnabled.collectAsStateWithLifecycle()

    var showQuickMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search manga...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .testTag("search_bar"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box {
                IconButton(
                    onClick = { showQuickMenu = true },
                    modifier = Modifier.testTag("three_dots_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Quick Menu",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                DropdownMenu(
                    expanded = showQuickMenu,
                    onDismissRequest = { showQuickMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = {
                            showQuickMenu = false
                            onNavigateToSettings()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Refresh Feed") },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        onClick = {
                            showQuickMenu = false
                            viewModel.loadHomeData()
                            Toast.makeText(context, "Refreshing feed...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        if (searchQuery.isNotBlank()) {
            ActiveSearchResults(
                searchState = searchState,
                navController = navController
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 0.dp)
            ) {
                if (kcEnabled) {
                    item {
                        SourceLaneHeader(title = "KomikCast") {}
                        SourceMangaLane(sourceState = kcPopular, navController = navController)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (sgEnabled) {
                    item {
                        SourceLaneHeader(title = "Shinigami") {}
                        SourceMangaLane(sourceState = sgPopular, navController = navController)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (!kcEnabled && !sgEnabled) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.LayersClear,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "All sources disabled in settings",
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                TextButton(onClick = onNavigateToSettings) {
                                    Text("Open Settings")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourceLaneHeader(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(4.dp, 20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "View more",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SourceMangaLane(sourceState: UiState<List<Manga>>, navController: NavController) {
    when (sourceState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 3.dp)
            }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sourceState.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        }
        is UiState.Success -> {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sourceState.data, key = { it.id }) { manga ->
                    MangaGridCard(manga = manga, navController = navController)
                }
            }
        }
        else -> {}
    }
}

@Composable
fun VerticalMangaGrid(mangas: List<Manga>, navController: NavController, modifier: Modifier = Modifier) {
    GridCellsAdaptive(items = mangas, columns = 3, modifier = modifier.padding(bottom = 0.dp)) { manga ->
        MangaGridCard(manga = manga, navController = navController, modifier = Modifier.padding(4.dp))
    }
}

@Composable
fun <T> GridCellsAdaptive(items: List<T>, columns: Int, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        val rows = items.chunked(columns)
        items(rows) { rowItems ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) { content(item) }
                }
                val remainder = columns - rowItems.size
                if (remainder > 0) repeat(remainder) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun ActiveSearchResults(searchState: UiState<List<Manga>>, navController: NavController) {
    when (searchState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Error icon",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = searchState.message,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        is UiState.Success -> {
            VerticalMangaGrid(mangas = searchState.data, navController = navController)
        }
        UiState.Idle -> {}
    }
}
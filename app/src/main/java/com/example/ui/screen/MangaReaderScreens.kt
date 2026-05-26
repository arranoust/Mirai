package com.example.ui.screen

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.launch
import com.example.data.model.Chapter
import com.example.data.model.Manga
import com.example.data.model.SavedManga
import com.example.ui.viewmodel.MangaViewModel
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.titleFromSlug

// List of seed colors for our picker
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
                items(sourceState.data) { manga ->
                    MangaGridCard(manga = manga, navController = navController)
                }
            }
        }
        else -> {}
    }
}

@Composable
fun MangaGridCard(manga: Manga, navController: NavController, modifier: Modifier = Modifier) {
    Card(
        onClick = { navController.navigate("detail/${manga.source}/${manga.slug}") },
        modifier = modifier.width(125.dp).testTag("manga_card_${manga.slug}"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = manga.thumbnailUrl ?: "",
                    contentDescription = manga.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                                startY = 120f
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = manga.source.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            Text(
                text = manga.title,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

// -------------------------------------------------------------------------------------------------
// LIBRARY SCREEN
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: MangaViewModel, navController: NavController) {
    val savedMangaList by viewModel.savedMangaList.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        
        TopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0), 
            title = { Text("Library", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        if (savedMangaList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(), 
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Book,
                        contentDescription = "Empty library icon",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your library is empty",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bookmark or save mangas from the Home page list to read them offline and track progress.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val normalMangas = savedMangaList.map {
                Manga(
                    id = it.id, slug = it.slug, source = it.source, title = it.title,
                    thumbnailUrl = it.thumbnailUrl, author = it.author, status = it.status,
                    description = it.description, genres = it.genres
                )
            }

            GridCellsAdaptive(items = normalMangas, columns = 3, modifier = Modifier.fillMaxSize().padding(bottom = 0.dp) 
            ) { manga ->
                MangaGridCard(manga = manga, navController = navController, modifier = Modifier.padding(4.dp))
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// MANGA DETAIL SCREEN
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailScreen(
    source: String,
    slug: String,
    viewModel: MangaViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    var isMangaSaved by remember { mutableStateOf(false) }

    LaunchedEffect(source, slug) {
        viewModel.loadMangaDetail(source, slug, null)
    }

    val detailManga by viewModel.detailManga.collectAsStateWithLifecycle()
    val detailChapters by viewModel.detailChapters.collectAsStateWithLifecycle()
    val loading by viewModel.detailLoading.collectAsStateWithLifecycle()
    val error by viewModel.detailError.collectAsStateWithLifecycle()

    val savedList by viewModel.savedMangaList.collectAsStateWithLifecycle()
    val currentSavedItem = remember(savedList, source, slug) {
        savedList.find { it.id == "$source:$slug" }
    }
    isMangaSaved = currentSavedItem != null

    // Observe hasil aksi save/remove dari ViewModel, lalu tampilkan Toast
    val libraryAction by viewModel.libraryActionResult.collectAsStateWithLifecycle()
    LaunchedEffect(libraryAction) {
        when (libraryAction) {
            is MangaViewModel.LibraryAction.Saved -> {
                Toast.makeText(context, "Saved to Library!", Toast.LENGTH_SHORT).show()
                viewModel.onLibraryActionHandled()
            }
            is MangaViewModel.LibraryAction.Removed -> {
                Toast.makeText(context, "Removed from library", Toast.LENGTH_SHORT).show()
                viewModel.onLibraryActionHandled()
            }
            is MangaViewModel.LibraryAction.Error -> {
                val msg = (libraryAction as MangaViewModel.LibraryAction.Error).message
                Toast.makeText(context, "Error: $msg", Toast.LENGTH_SHORT).show()
                viewModel.onLibraryActionHandled()
            }
            null -> {}
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0), 
                title = {
                    Text(
                        detailManga?.title ?: "Manga Detail",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val manga = detailManga
                            if (manga != null) {
                                viewModel.toggleMangaSaved(manga, isMangaSaved, slug)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isMangaSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save / Remove",
                            tint = if (isMangaSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), 
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Error: $error", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        } else if (detailManga != null) {
            val manga = detailManga!!
            val displayTitle = manga.title.takeIf { it.isNotBlank() } ?: titleFromSlug(slug)

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(bottom = 0.dp)
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(110.dp, 155.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (!manga.thumbnailUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = manga.thumbnailUrl,
                                    contentDescription = manga.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    modifier = Modifier.align(Alignment.Center).size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Source: $source",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Status: ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = manga.status ?: "Ongoing",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Author: ${manga.author ?: "Unknown"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                item {
                    if (!manga.description.isNullOrBlank()) {
                        var isExpanded by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(text = "Synopsis", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = manga.description!!,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { isExpanded = !isExpanded }
                            )
                        }
                    }
                }

                if (currentSavedItem != null && currentSavedItem.lastReadChapterName != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Resume Reading", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(currentSavedItem.lastReadChapterName!!, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Chapters (${detailChapters.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                if (detailChapters.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No chapters available.", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    items(detailChapters) { chapter ->
                        val isRead = currentSavedItem?.lastReadChapterId == chapter.id
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = chapter.name,
                                    fontWeight = if (isRead) FontWeight.Normal else FontWeight.Medium,
                                    color = if (isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            supportingContent = {
                                if (!chapter.dateUpload.isNullOrEmpty()) {
                                    Text(chapter.dateUpload, fontSize = 11.sp)
                                }
                            },
                            trailingContent = {
                                if (isRead) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Read",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            modifier = Modifier
                                .clickable { navController.navigate("reader/$source/$slug/${chapter.id}") }
                                .padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// MANGA READER SCREEN
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaReaderScreen(
    source: String,
    slug: String,
    chapterId: String,
    viewModel: MangaViewModel,
    navController: NavController
) {
    val readerMode by viewModel.settingsManager.readerMode.collectAsStateWithLifecycle()
    val tapToZoom by viewModel.settingsManager.tapToZoom.collectAsStateWithLifecycle()

    val chaptersList by viewModel.detailChapters.collectAsStateWithLifecycle()
    val pagesState by viewModel.readerPages.collectAsStateWithLifecycle()
    val currentChapter by viewModel.readerCurrentChapter.collectAsStateWithLifecycle()

    val mangaDetail by viewModel.detailManga.collectAsStateWithLifecycle()
    val mangaTitle = mangaDetail?.title ?: titleFromSlug(slug)

    val curIndex = remember(chaptersList, chapterId) {
        chaptersList.indexOfFirst { it.id == chapterId }
    }

    LaunchedEffect(chapterId) {
        val ch = chaptersList.find { it.id == chapterId } ?: Chapter(chapterId, "Chapter $chapterId")
        viewModel.loadChapterPages(source, slug, ch)
    }

    var zoomScale by remember { mutableStateOf(1f) }
    var zoomOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0), // FIX: Mencegah gap di atas TopAppBar karena nested padding MainActivity
                title = {
                    Column {
                        Text(mangaTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(currentChapter?.name ?: "Loading...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (zoomScale != 1f) {
                        IconButton(onClick = {
                            zoomScale = 1f
                            zoomOffset = androidx.compose.ui.geometry.Offset.Zero
                        }) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Reset Zoom")
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0), // FIX: Mencegah gap tambahan di bawah BottomAppBar internal karena bentrokan insets
                containerColor = MaterialTheme.colorScheme.surface,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (curIndex != -1 && curIndex < chaptersList.size - 1) {
                                val prevChap = chaptersList[curIndex + 1]
                                navController.navigate("reader/$source/$slug/${prevChap.id}") {
                                    popUpTo("reader/$source/$slug/$chapterId") { inclusive = true }
                                }
                            }
                        },
                        enabled = curIndex != -1 && curIndex < chaptersList.size - 1,
                        modifier = Modifier.testTag("prev_chapter_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Chapter")
                    }

                    IconButton(onClick = {
                        val ch = currentChapter
                        if (ch != null) viewModel.loadChapterPages(source, slug, ch)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Pages")
                    }

                    IconButton(
                        onClick = {
                            if (curIndex > 0) {
                                val nextChap = chaptersList[curIndex - 1]
                                navController.navigate("reader/$source/$slug/${nextChap.id}") {
                                    popUpTo("reader/$source/$slug/$chapterId") { inclusive = true }
                                }
                            }
                        },
                        enabled = curIndex > 0,
                        modifier = Modifier.testTag("next_chapter_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Chapter")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            when (val stateVal = pagesState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stateVal.message, color = Color.White, textAlign = TextAlign.Center)
                    }
                }
                is UiState.Success -> {
                    val imageUrls = stateVal.data
                    val scaleModifier = Modifier
                        .pointerInput(tapToZoom) {
                            if (tapToZoom) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    zoomScale = (zoomScale * zoom).coerceIn(1f, 4f)
                                    zoomOffset = if (zoomScale > 1f) zoomOffset + pan
                                                 else androidx.compose.ui.geometry.Offset.Zero
                                }
                            }
                        }
                        .graphicsLayer(scaleX = zoomScale, scaleY = zoomScale, translationX = zoomOffset.x, translationY = zoomOffset.y)

                    if (readerMode == "vertical") {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().then(scaleModifier),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(imageUrls) { imageUrl ->
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Manga Page",
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxSize().then(scaleModifier),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(imageUrls) { imageUrl ->
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Manga Page",
                                    modifier = Modifier.fillMaxHeight().wrapContentWidth(),
                                    contentScale = ContentScale.FillHeight
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SETTINGS SCREEN
// -------------------------------------------------------------------------------------------------
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
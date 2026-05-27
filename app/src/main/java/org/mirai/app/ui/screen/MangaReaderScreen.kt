package org.mirai.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import org.mirai.app.data.model.Chapter
import org.mirai.app.ui.viewmodel.MangaViewModel
import org.mirai.app.ui.viewmodel.UiState
import org.mirai.app.ui.viewmodel.titleFromSlug
import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.request.ImageRequest

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
    var barsVisible by remember { mutableStateOf(true) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
                        .pointerInput(tapToZoom) {
                            detectTapGestures(
                                onTap = {
                                    barsVisible = !barsVisible
                                },
                                onDoubleTap = {
                                    if (tapToZoom) {
                                        if (zoomScale > 1f) {
                                            zoomScale = 1f
                                            zoomOffset = androidx.compose.ui.geometry.Offset.Zero
                                        } else {
                                            zoomScale = 2.5f
                                        }
                                    }
                                }
                            )
                        }
                        .graphicsLayer(scaleX = zoomScale, scaleY = zoomScale, translationX = zoomOffset.x, translationY = zoomOffset.y)

                    val context = LocalContext.current

                    if (readerMode == "vertical") {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().then(scaleModifier),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            userScrollEnabled = zoomScale == 1f
                        ) {
                            items(imageUrls) { imageUrl ->
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUrl)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .crossfade(true) 
                                        .build(),
                                    contentDescription = "Manga Page",
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxSize().then(scaleModifier),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            userScrollEnabled = zoomScale == 1f
                        ) {
                            items(imageUrls) { imageUrl ->
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUrl)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .crossfade(true) 
                                        .build(),
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

            // Animated TopAppBar Overlay
            AnimatedVisibility(
                visible = barsVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopAppBar(
                    windowInsets = WindowInsets.statusBars,
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            }

            // Animated BottomAppBar Overlay
            AnimatedVisibility(
                visible = barsVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                BottomAppBar(
                    windowInsets = WindowInsets.navigationBars,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
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
        }
    }
}
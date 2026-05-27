package org.mirai.app.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import org.mirai.app.ui.viewmodel.MangaViewModel
import org.mirai.app.ui.viewmodel.titleFromSlug

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
package org.mirai.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.mirai.app.data.model.Manga
import org.mirai.app.ui.viewmodel.MangaViewModel

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
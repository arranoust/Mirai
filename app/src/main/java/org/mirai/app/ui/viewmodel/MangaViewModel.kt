package org.mirai.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import org.mirai.app.data.local.AppDatabase
import org.mirai.app.data.local.SettingsManager
import org.mirai.app.data.model.Chapter
import org.mirai.app.data.model.Manga
import org.mirai.app.data.model.SavedManga
import org.mirai.app.data.repository.MangaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class MangaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = MangaRepository(database.savedMangaDao()) // ← sekarang private
    val settingsManager = SettingsManager(application)

    // HOME STATES
    private val _komikCastPopular = MutableStateFlow<UiState<List<Manga>>>(UiState.Loading)
    val komikCastPopular: StateFlow<UiState<List<Manga>>> = _komikCastPopular

    private val _shinigamiPopular = MutableStateFlow<UiState<List<Manga>>>(UiState.Loading)
    val shinigamiPopular: StateFlow<UiState<List<Manga>>> = _shinigamiPopular

    val savedMangaList: StateFlow<List<SavedManga>> = repository.savedMangas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // SEARCH STATES
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val searchResult: StateFlow<UiState<List<Manga>>> = _searchQuery
        .debounce { query -> if (query.isBlank()) 0L else 400L }
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flow { emit(UiState.Idle) }
            } else {
                flow {
                    emit(UiState.Loading)
                    val combinedList = mutableListOf<Manga>()
                    val kcEnabled = settingsManager.komikCastEnabled.value
                    val sgEnabled = settingsManager.shinigamiEnabled.value
                    if (kcEnabled) {
                        try {
                            combinedList.addAll(repository.search("KomikCast", query, 1))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    if (sgEnabled) {
                        try {
                            combinedList.addAll(repository.search("Shinigami", query, 1))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    emit(
                        if (combinedList.isNotEmpty()) UiState.Success(combinedList)
                        else UiState.Error("No matching manga found.")
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Idle)

    // DETAIL STATES
    private val _detailManga = MutableStateFlow<Manga?>(null)
    val detailManga: StateFlow<Manga?> = _detailManga

    private val _detailChapters = MutableStateFlow<List<Chapter>>(emptyList())
    val detailChapters: StateFlow<List<Chapter>> = _detailChapters

    private val _detailLoading = MutableStateFlow(false)
    val detailLoading: StateFlow<Boolean> = _detailLoading

    private val _detailError = MutableStateFlow<String?>(null)
    val detailError: StateFlow<String?> = _detailError

    // READER STATES
    private val _readerPages = MutableStateFlow<UiState<List<String>>>(UiState.Loading)
    val readerPages: StateFlow<UiState<List<String>>> = _readerPages

    private val _readerCurrentChapter = MutableStateFlow<Chapter?>(null)
    val readerCurrentChapter: StateFlow<Chapter?> = _readerCurrentChapter

    // LIBRARY ACTION STATES
    private val _libraryActionResult = MutableStateFlow<LibraryAction?>(null)
    val libraryActionResult: StateFlow<LibraryAction?> = _libraryActionResult

    sealed class LibraryAction {
        object Saved : LibraryAction()
        object Removed : LibraryAction()
        data class Error(val message: String) : LibraryAction()
    }

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _komikCastPopular.value = UiState.Loading
            _shinigamiPopular.value = UiState.Loading

            launch {
                val kcList = repository.getPopular("KomikCast", 1)
                _komikCastPopular.value = if (kcList.isNotEmpty()) {
                    UiState.Success(kcList)
                } else {
                    UiState.Error("Failed to load KomikCast popularity list.")
                }
            }

            launch {
                val sgList = repository.getPopular("Shinigami", 1)
                _shinigamiPopular.value = if (sgList.isNotEmpty()) {
                    UiState.Success(sgList)
                } else {
                    UiState.Error("Failed to load Shinigami popularity list.")
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun loadMangaDetail(source: String, slug: String, fallbackManga: Manga?) {
        viewModelScope.launch {
            _detailLoading.value = true
            _detailError.value = null
            _detailManga.value = fallbackManga

            try {
                val info = repository.getMangaDetails(source, slug)
                val fullManga = if (fallbackManga != null) {
                    info.copy(title = fallbackManga.title, thumbnailUrl = fallbackManga.thumbnailUrl)
                } else {
                    info
                }
                _detailManga.value = fullManga

                val chapters = repository.getChapters(source, slug)
                _detailChapters.value = chapters
                _detailLoading.value = false
            } catch (e: Exception) {
                _detailError.value = e.message ?: "Failed to load details"
                _detailLoading.value = false
            }
        }
    }

    fun toggleMangaSaved(manga: Manga, isSaved: Boolean, slug: String) {
        viewModelScope.launch {
            try {
                if (isSaved) {
                    repository.removeManga(manga.id)
                    _libraryActionResult.value = LibraryAction.Removed
                } else {
                    val title = manga.title.takeIf { it.isNotBlank() } ?: titleFromSlug(slug)
                    repository.saveManga(manga.copy(title = title))
                    _libraryActionResult.value = LibraryAction.Saved
                }
            } catch (e: Exception) {
                _libraryActionResult.value = LibraryAction.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun onLibraryActionHandled() {
        _libraryActionResult.value = null
    }

    fun loadChapterPages(source: String, slug: String, chapter: Chapter) {
        viewModelScope.launch {
            _readerPages.value = UiState.Loading
            _readerCurrentChapter.value = chapter
            try {
                val pages = repository.getChapterPages(source, slug, chapter.id)
                if (pages.isNotEmpty()) {
                    _readerPages.value = UiState.Success(pages)
                    repository.updateLastReadChapter("$source:$slug", chapter.id, chapter.name)
                    settingsManager.simulateCacheIncrease()
                } else {
                    _readerPages.value = UiState.Error("No pages found inside this chapter.")
                }
            } catch (e: Exception) {
                _readerPages.value = UiState.Error(e.message ?: "Failed to load reader pages.")
            }
        }
    }
}

fun titleFromSlug(slug: String): String {
    return slug.replace("-", " ").replace("_", " ")
        .split(" ")
        .joinToString(" ") { word -> word.lowercase().replaceFirstChar { it.uppercase() } }
}
package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsManager
import com.example.data.model.Chapter
import com.example.data.model.Manga
import com.example.data.model.SavedManga
import com.example.data.repository.MangaRepository
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
    val repository = MangaRepository(database.savedMangaDao())
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

    private val _searchResult = MutableStateFlow<UiState<List<Manga>>>(UiState.Idle)
    val searchResult: StateFlow<UiState<List<Manga>>> = _searchResult

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
        if (query.isBlank()) {
            _searchResult.value = UiState.Idle
            return
        }

        viewModelScope.launch {
            _searchResult.value = UiState.Loading
            val combinedList = mutableListOf<Manga>()

            // Query enabled sources in parallel
            val kcEnabled = settingsManager.komikCastEnabled.value
            val sgEnabled = settingsManager.shinigamiEnabled.value

            if (kcEnabled) {
                try {
                    val list = repository.search("KomikCast", query, 1)
                    combinedList.addAll(list)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (sgEnabled) {
                try {
                    val list = repository.search("Shinigami", query, 1)
                    combinedList.addAll(list)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (combinedList.isNotEmpty()) {
                _searchResult.value = UiState.Success(combinedList)
            } else {
                _searchResult.value = UiState.Error("No matching manga found.")
            }
        }
    }

    fun loadMangaDetail(source: String, slug: String, fallbackManga: Manga?) {
        viewModelScope.launch {
            _detailLoading.value = true
            _detailError.value = null
            _detailManga.value = fallbackManga // Start with fallback title/thumbnail

            try {
                // Fetch details
                val info = repository.getMangaDetails(source, slug)
                val fullManga = if (fallbackManga != null) {
                    info.copy(title = fallbackManga.title, thumbnailUrl = fallbackManga.thumbnailUrl)
                } else {
                    info
                }
                _detailManga.value = fullManga

                // Fetch chapters
                val chapters = repository.getChapters(source, slug)
                _detailChapters.value = chapters
                _detailLoading.value = false
            } catch (e: Exception) {
                _detailError.value = e.message ?: "Failed to load details"
                _detailLoading.value = false
            }
        }
    }

    fun loadChapterPages(source: String, slug: String, chapter: Chapter) {
        viewModelScope.launch {
            _readerPages.value = UiState.Loading
            _readerCurrentChapter.value = chapter
            try {
                val pages = repository.getChapterPages(source, slug, chapter.id)
                if (pages.isNotEmpty()) {
                    _readerPages.value = UiState.Success(pages)
                    // Mark progress in Room db
                    repository.updateLastReadChapter("$source:$slug", chapter.id, chapter.name)
                    // Increase cache measurement slightly
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

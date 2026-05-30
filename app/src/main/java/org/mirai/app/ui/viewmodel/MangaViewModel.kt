package org.mirai.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mirai.app.data.local.AppDatabase
import org.mirai.app.data.local.SettingsManager
import org.mirai.app.data.model.Chapter
import org.mirai.app.data.model.Manga
import org.mirai.app.data.model.SavedManga
import org.mirai.app.data.repository.MangaRepository

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class MangaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = MangaRepository(database.savedMangaDao())
    val settingsManager = SettingsManager(application)

    private val _homeStates = MutableStateFlow<Map<String, UiState<List<Manga>>>>(
        mapOf("KomikCast" to UiState.Loading, "Shinigami" to UiState.Loading)
    )

    val komikCastLatest: StateFlow<UiState<List<Manga>>> = _homeStates
        .map { it["KomikCast"] ?: UiState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val shinigamiLatest: StateFlow<UiState<List<Manga>>> = _homeStates
        .map { it["Shinigami"] ?: UiState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val savedMangaList: StateFlow<List<SavedManga>> = repository.savedMangas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResult: StateFlow<UiState<List<Manga>>> = _searchQuery
        .debounce(400L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(UiState.Idle)
            } else {
                flow {
                    emit(UiState.Loading)
                    try {
                        val results = mutableListOf<Manga>()
                        val kcEnabled = settingsManager.komikCastEnabled.value
                        val sgEnabled = settingsManager.shinigamiEnabled.value
                        if (kcEnabled) results.addAll(repository.search("KomikCast", query, 1))
                        if (sgEnabled) results.addAll(repository.search("Shinigami", query, 1))
                        emit(
                            if (results.isNotEmpty()) UiState.Success(results)
                            else UiState.Error("Tidak ada komik yang ditemukan.")
                        )
                    } catch (e: Exception) {
                        emit(UiState.Error(e.message ?: "Pencarian gagal."))
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Idle)

    private val _detailManga = MutableStateFlow<Manga?>(null)
    val detailManga: StateFlow<Manga?> = _detailManga.asStateFlow()

    private val _detailChapters = MutableStateFlow<List<Chapter>>(emptyList())
    val detailChapters: StateFlow<List<Chapter>> = _detailChapters.asStateFlow()

    private val _detailLoading = MutableStateFlow(false)
    val detailLoading: StateFlow<Boolean> = _detailLoading.asStateFlow()

    private val _detailError = MutableStateFlow<String?>(null)
    val detailError: StateFlow<String?> = _detailError.asStateFlow()

    private val _readerPages = MutableStateFlow<UiState<List<String>>>(UiState.Loading)
    val readerPages: StateFlow<UiState<List<String>>> = _readerPages.asStateFlow()

    private val _readerCurrentChapter = MutableStateFlow<Chapter?>(null)
    val readerCurrentChapter: StateFlow<Chapter?> = _readerCurrentChapter.asStateFlow()

    private val _libraryActionResult = MutableStateFlow<LibraryAction?>(null)
    val libraryActionResult: StateFlow<LibraryAction?> = _libraryActionResult.asStateFlow()

    sealed class LibraryAction {
        object Saved : LibraryAction()
        object Removed : LibraryAction()
        data class Error(val message: String) : LibraryAction()
    }

    private val _browseList = MutableStateFlow<UiState<List<Manga>>>(UiState.Loading)
    val browseList: StateFlow<UiState<List<Manga>>> = _browseList.asStateFlow()

    private var detailJob: Job? = null
    private var readerJob: Job? = null

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _homeStates.value = mapOf(
                "KomikCast" to UiState.Loading,
                "Shinigami" to UiState.Loading
            )
            launch {
                val state = runCatching { repository.getLatest("KomikCast", 1) }.fold(
                    onSuccess = { if (it.isNotEmpty()) UiState.Success(it) else UiState.Error("Tidak ada data dari KomikCast.") },
                    onFailure = { UiState.Error(it.message ?: "KomikCast gagal dimuat.") }
                )
                _homeStates.update { it + ("KomikCast" to state) }
            }
            launch {
                val state = runCatching { repository.getLatest("Shinigami", 1) }.fold(
                    onSuccess = { if (it.isNotEmpty()) UiState.Success(it) else UiState.Error("Tidak ada data dari Shinigami.") },
                    onFailure = { UiState.Error(it.message ?: "Shinigami gagal dimuat.") }
                )
                _homeStates.update { it + ("Shinigami" to state) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        if (_searchQuery.value != query) _searchQuery.value = query
    }

    fun loadBrowse(source: String, page: Int = 1, filter: String = "all") {
        viewModelScope.launch {
            _browseList.value = UiState.Loading
            _browseList.value = runCatching {
                when (filter) {
                    "manga"  -> repository.search(source, "manga", page)
                    "manhwa" -> repository.search(source, "manhwa", page)
                    "manhua" -> repository.search(source, "manhua", page)
                    else     -> repository.getPopular(source, page)
                }
            }.fold(
                onSuccess = { if (it.isNotEmpty()) UiState.Success(it) else UiState.Error("Tidak ada data.") },
                onFailure = { UiState.Error(it.message ?: "Gagal memuat.") }
            )
        }
    }

    fun loadMangaDetail(source: String, slug: String, fallbackManga: Manga?) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _detailLoading.value = true
            _detailError.value = null

            val id = "$source:$slug"
            val resolvedFallback = fallbackManga ?: resolveFallbackManga(id, source, slug)
            _detailManga.value = resolvedFallback

            runCatching {
                val detailDeferred = async { repository.getMangaDetails(source, slug) }
                val chaptersDeferred = async { repository.getChapters(source, slug) }
                Pair(detailDeferred.await(), chaptersDeferred.await())
            }.fold(
                onSuccess = { (info, chapters) ->
                    val merged = resolvedFallback?.let { fb ->
                        info.copy(
                            title = fb.title.takeIf { it.isNotBlank() } ?: info.title,
                            thumbnailUrl = fb.thumbnailUrl ?: info.thumbnailUrl
                        )
                    } ?: info
                    _detailManga.value = merged
                    _detailChapters.value = chapters
                },
                onFailure = { _detailError.value = it.message ?: "Gagal memuat detail." }
            )

            _detailLoading.value = false
        }
    }

    private suspend fun resolveFallbackManga(id: String, source: String, slug: String): Manga? {
        repository.getSavedManga(id)?.let { return it.toManga() }
        val homeList = when (source) {
            "KomikCast" -> (komikCastLatest.value as? UiState.Success)?.data
            else        -> (shinigamiLatest.value as? UiState.Success)?.data
        }
        homeList?.find { it.slug == slug }?.let { return it }
        return (searchResult.value as? UiState.Success)?.data
            ?.find { it.slug == slug && it.source == source }
    }

    fun toggleMangaSaved(manga: Manga, isSaved: Boolean, slug: String) {
        viewModelScope.launch {
            runCatching {
                if (isSaved) {
                    repository.removeManga(manga.id)
                    _libraryActionResult.value = LibraryAction.Removed
                } else {
                    val title = manga.title.takeIf { it.isNotBlank() } ?: titleFromSlug(slug)
                    repository.saveManga(manga.copy(title = title))
                    _libraryActionResult.value = LibraryAction.Saved
                }
            }.onFailure { _libraryActionResult.value = LibraryAction.Error(it.message ?: "Unknown error") }
        }
    }

    fun onLibraryActionHandled() {
        _libraryActionResult.value = null
    }

    fun loadChapterPages(source: String, slug: String, chapter: Chapter) {
        readerJob?.cancel()
        readerJob = viewModelScope.launch {
            _readerPages.value = UiState.Loading
            _readerCurrentChapter.value = chapter
            runCatching { repository.getChapterPages(source, slug, chapter.id) }.fold(
                onSuccess = { pages ->
                    if (pages.isNotEmpty()) {
                        _readerPages.value = UiState.Success(pages)
                        launch {
                            repository.updateLastReadChapter("$source:$slug", chapter.id, chapter.name)
                            settingsManager.simulateCacheIncrease()
                        }
                    } else {
                        _readerPages.value = UiState.Error("Tidak ada halaman di chapter ini.")
                    }
                },
                onFailure = { _readerPages.value = UiState.Error(it.message ?: "Gagal memuat halaman.") }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        detailJob?.cancel()
        readerJob?.cancel()
    }
}

fun titleFromSlug(slug: String): String =
    slug.replace('-', ' ').replace('_', ' ')
        .split(' ')
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }

private fun SavedManga.toManga() = Manga(
    id = id, slug = slug, source = source, title = title,
    thumbnailUrl = thumbnailUrl, author = author, status = status,
    description = description, genres = genres
)
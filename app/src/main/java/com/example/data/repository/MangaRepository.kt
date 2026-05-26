package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.*
import com.example.data.local.SavedMangaDao
import com.example.data.model.Chapter
import com.example.data.model.Manga
import com.example.data.model.SavedManga
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class MangaRepository(
    private val savedMangaDao: SavedMangaDao
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
            chain.proceed(req)
        }
        .build()

    private val komikCastService: KomikCastService = Retrofit.Builder()
        .baseUrl("https://be.komikcast.cc/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(KomikCastService::class.java)

    private val shinigamiService: ShinigamiService = Retrofit.Builder()
        .baseUrl("https://api.shngm.io/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(ShinigamiService::class.java)

    // Room Database Observables
    val savedMangas: Flow<List<SavedManga>> = savedMangaDao.getAllSavedManga()

    fun isMangaSaved(mangaId: String): Flow<Boolean> = savedMangaDao.isMangaSavedFlow(mangaId)

    suspend fun saveManga(manga: Manga) {
        savedMangaDao.insertManga(manga.toSavedManga())
    }

    suspend fun removeManga(mangaId: String) {
        savedMangaDao.deleteMangaById(mangaId)
    }

    suspend fun updateLastReadChapter(mangaId: String, chapterId: String, chapterName: String) {
        savedMangaDao.updateLastReadChapter(mangaId, chapterId, chapterName)
    }

    suspend fun getSavedManga(mangaId: String): SavedManga? {
        return savedMangaDao.getSavedMangaById(mangaId)
    }

    // Network Source Calls
    suspend fun getPopular(source: String, page: Int): List<Manga> {
        return try {
            if (source == "KomikCast") {
                val response = komikCastService.getPopular(page = page.toString())
                response.data.map { item ->
                    val slug = item.data.slug ?: item.id.toString()
                    Manga(
                        id = "komikcast:$slug",
                        slug = slug,
                        source = "KomikCast",
                        title = item.data.title,
                        thumbnailUrl = item.data.coverImage,
                        author = item.data.author,
                        status = item.data.status,
                        description = item.data.synopsis,
                        genres = item.data.genres?.joinToString { it.data.name }
                    )
                }
            } else {
                val response = shinigamiService.getMangaList(page = page, sort = "popularity")
                response.data.map { item ->
                    val mangaId = item.mangaId ?: ""
                    Manga(
                        id = "shinigami:$mangaId",
                        slug = mangaId,
                        source = "Shinigami",
                        title = item.title ?: "Untitled",
                        thumbnailUrl = item.coverImageUrl,
                        status = "Ongoing"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MangaRepository", "Error getting popular for $source: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getLatest(source: String, page: Int): List<Manga> {
        return try {
            if (source == "KomikCast") {
                val response = komikCastService.getLatest(page = page.toString())
                response.data.map { item ->
                    val slug = item.data.slug ?: item.id.toString()
                    Manga(
                        id = "komikcast:$slug",
                        slug = slug,
                        source = "KomikCast",
                        title = item.data.title,
                        thumbnailUrl = item.data.coverImage,
                        author = item.data.author,
                        status = item.data.status,
                        description = item.data.synopsis,
                        genres = item.data.genres?.joinToString { it.data.name }
                    )
                }
            } else {
                val response = shinigamiService.getMangaList(page = page, sort = "latest")
                response.data.map { item ->
                    val mangaId = item.mangaId ?: ""
                    Manga(
                        id = "shinigami:$mangaId",
                        slug = mangaId,
                        source = "Shinigami",
                        title = item.title ?: "Untitled",
                        thumbnailUrl = item.coverImageUrl,
                        status = "Ongoing"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MangaRepository", "Error getting latest for $source: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun search(source: String, query: String, page: Int): List<Manga> {
        return try {
            if (source == "KomikCast") {
                val filter = "title=like=\"$query\",nativeTitle=like=\"$query\""
                val response = komikCastService.search(page = page.toString(), filter = filter)
                response.data.map { item ->
                    val slug = item.data.slug ?: item.id.toString()
                    Manga(
                        id = "komikcast:$slug",
                        slug = slug,
                        source = "KomikCast",
                        title = item.data.title,
                        thumbnailUrl = item.data.coverImage,
                        author = item.data.author,
                        status = item.data.status,
                        description = item.data.synopsis,
                        genres = item.data.genres?.joinToString { it.data.name }
                    )
                }
            } else {
                val response = shinigamiService.searchManga(page = page, query = query)
                response.data.map { item ->
                    val mangaId = item.mangaId ?: ""
                    Manga(
                        id = "shinigami:$mangaId",
                        slug = mangaId,
                        source = "Shinigami",
                        title = item.title ?: "Untitled",
                        thumbnailUrl = item.coverImageUrl,
                        status = "Ongoing"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MangaRepository", "Error searching for $source with $query: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getMangaDetails(source: String, slug: String): Manga {
        return try {
            if (source == "KomikCast") {
                val response = komikCastService.getDetail(slug)
                val item = response.data
                Manga(
                    id = "komikcast:$slug",
                    slug = slug,
                    source = "KomikCast",
                    title = item.data.title,
                    thumbnailUrl = item.data.coverImage,
                    author = item.data.author ?: "Unknown Author",
                    status = item.data.status ?: "Ongoing",
                    description = item.data.synopsis ?: "No synopsis available",
                    genres = item.data.genres?.joinToString { it.data.name } ?: "Manga"
                )
            } else {
                val response = shinigamiService.getMangaDetail(slug)
                val item = response.data
                val genresList = item.taxonomy["Genre"]?.map { it.name }.orEmpty()
                val formatList = item.taxonomy["Format"]?.map { it.name }.orEmpty()
                val genreText = (genresList + formatList).distinct().joinToString()

                val authorsList = item.taxonomy["Author"]?.map { it.name }.orEmpty()
                val artistsList = item.taxonomy["Artist"]?.map { it.name }.orEmpty()
                val authorText = (authorsList + artistsList).distinct().joinToString().takeIf { it.isNotBlank() } ?: "Unknown"

                Manga(
                    id = "shinigami:$slug",
                    slug = slug,
                    source = "Shinigami",
                    title = "", // Title is already known from navigation/grid, details return details
                    thumbnailUrl = null,
                    author = authorText,
                    status = when (item.status) {
                        1 -> "Ongoing"
                        2 -> "Completed"
                        3 -> "Hiatus"
                        else -> "Unknown"
                    },
                    description = item.description,
                    genres = genreText
                )
            }
        } catch (e: Exception) {
            Log.e("MangaRepository", "Error details for $source: ${e.message}", e)
            // Fallback mock/empty item so app doesn't crash
            Manga(
                id = "$source:$slug",
                slug = slug,
                source = source,
                title = "Error Loading details",
                thumbnailUrl = null,
                author = "Unknown",
                status = "Unknown",
                description = "Could not fetch details. Check your internet connection."
            )
        }
    }

    suspend fun getChapters(source: String, slug: String): List<Chapter> {
        return try {
            if (source == "KomikCast") {
                val response = komikCastService.getChapters(slug)
                response.data.map { item ->
                    val index = item.data.index ?: item.chapterIndex ?: 0f
                    val titleSuffix = if (!item.data.title.isNullOrBlank()) ": ${item.data.title}" else ""
                    Chapter(
                        id = index.toString(),
                        name = "Chapter ${index.toString().replace(".0", "")}$titleSuffix",
                        dateUpload = item.createdAt?.substringBefore("T"),
                        number = index
                    )
                }
            } else {
                val response = shinigamiService.getChapterList(slug)
                response.data.map { item ->
                    Chapter(
                        id = item.chapterId,
                        name = "Chapter ${item.name.toString().replace(".0", "")} ${item.title}",
                        dateUpload = item.date.substringBefore("T"),
                        number = item.name.toFloat()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MangaRepository", "Error chapters for $source: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getChapterPages(source: String, slug: String, chapterId: String): List<String> {
        return try {
            if (source == "KomikCast") {
                val response = komikCastService.getChapterDetail(slug, chapterId)
                response.data.data.images ?: emptyList()
            } else {
                val response = shinigamiService.getChapterPages(chapterId)
                val data = response.pageList.chapterPage
                val path = data.path
                val cdnUrl = "https://storage.shngm.id"
                data.pages.map { imageName ->
                    "$cdnUrl$path$imageName"
                }
            }
        } catch (e: Exception) {
            Log.e("MangaRepository", "Error getting pages for $source chapter $chapterId: ${e.message}", e)
            emptyList()
        }
    }
}

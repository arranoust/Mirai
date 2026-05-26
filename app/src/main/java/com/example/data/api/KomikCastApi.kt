package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class KCGenreInfo(
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class KCGenreData(
    @Json(name = "data") val data: KCGenreInfo
)

@JsonClass(generateAdapter = true)
data class KCSeriesData(
    @Json(name = "slug") val slug: String?,
    @Json(name = "title") val title: String,
    @Json(name = "author") val author: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "synopsis") val synopsis: String?,
    @Json(name = "coverImage") val coverImage: String?,
    @Json(name = "genres") val genres: List<KCGenreData>?
)

@JsonClass(generateAdapter = true)
data class KCSeriesItem(
    @Json(name = "id") val id: Int,
    @Json(name = "data") val data: KCSeriesData
)

@JsonClass(generateAdapter = true)
data class KCMeta(
    @Json(name = "page") val page: Int?,
    @Json(name = "lastPage") val lastPage: Int?
)

@JsonClass(generateAdapter = true)
data class KCSeriesListResponse(
    @Json(name = "data") val data: List<KCSeriesItem>,
    @Json(name = "meta") val meta: KCMeta?
)

@JsonClass(generateAdapter = true)
data class KCSeriesDetailResponse(
    @Json(name = "data") val data: KCSeriesItem
)

@JsonClass(generateAdapter = true)
data class KCChapterData(
    @Json(name = "index") val index: Float?,
    @Json(name = "title") val title: String?,
    @Json(name = "images") val images: List<String>?
)

@JsonClass(generateAdapter = true)
data class KCChapterItem(
    @Json(name = "data") val data: KCChapterData,
    @Json(name = "createdAt") val createdAt: String?,
    @Json(name = "updatedAt") val updatedAt: String?,
    @Json(name = "chapterIndex") val chapterIndex: Float?
)

@JsonClass(generateAdapter = true)
data class KCChapterListResponse(
    @Json(name = "data") val data: List<KCChapterItem>
)

@JsonClass(generateAdapter = true)
data class KCChapterDetailResponse(
    @Json(name = "data") val data: KCChapterItem
)

interface KomikCastService {
    @GET("series")
    suspend fun getPopular(
        @Query("includeMeta") includeMeta: String = "true",
        @Query("sort") sort: String = "popularity",
        @Query("sortOrder") sortOrder: String = "desc",
        @Query("take") take: String = "24",
        @Query("page") page: String
    ): KCSeriesListResponse

    @GET("series")
    suspend fun getLatest(
        @Query("includeMeta") includeMeta: String = "true",
        @Query("sort") sort: String = "latest",
        @Query("sortOrder") sortOrder: String = "desc",
        @Query("take") take: String = "24",
        @Query("page") page: String
    ): KCSeriesListResponse

    @GET("series")
    suspend fun search(
        @Query("includeMeta") includeMeta: String = "true",
        @Query("take") take: String = "40",
        @Query("page") page: String,
        @Query("filter") filter: String
    ): KCSeriesListResponse

    @GET("series/{slug}")
    suspend fun getDetail(
        @Path("slug") slug: String
    ): KCSeriesDetailResponse

    @GET("series/{slug}/chapters")
    suspend fun getChapters(
        @Path("slug") slug: String
    ): KCChapterListResponse

    @GET("series/{slug}/chapters/{chapterIndex}")
    suspend fun getChapterDetail(
        @Path("slug") slug: String,
        @Path("chapterIndex") chapterIndex: String
    ): KCChapterDetailResponse
}

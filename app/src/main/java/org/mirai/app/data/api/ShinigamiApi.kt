package org.mirai.app.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class SGMetaDto(
    @Json(name = "page") val page: Int,
    @Json(name = "total_page") val totalPage: Int?
)

@JsonClass(generateAdapter = true)
data class SGBrowseItem(
    @Json(name = "cover_image_url") val coverImageUrl: String?,
    @Json(name = "manga_id") val mangaId: String?,
    @Json(name = "title") val title: String?
)

@JsonClass(generateAdapter = true)
data class SGBrowseResponse(
    @Json(name = "data") val data: List<SGBrowseItem>,
    @Json(name = "meta") val meta: SGMetaDto
)

@JsonClass(generateAdapter = true)
data class SGTaxonomyItem(
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class SGMangaDetailData(
    @Json(name = "description") val description: String = "",
    @Json(name = "status") val status: Int = 0,
    @Json(name = "taxonomy") val taxonomy: Map<String, List<SGTaxonomyItem>> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class SGMangaDetailResponse(
    @Json(name = "data") val data: SGMangaDetailData
)

@JsonClass(generateAdapter = true)
data class SGChapterItem(
    @Json(name = "release_date") val date: String = "",
    @Json(name = "chapter_title") val title: String = "",
    @Json(name = "chapter_number") val name: Double = 0.0,
    @Json(name = "chapter_id") val chapterId: String = ""
)

@JsonClass(generateAdapter = true)
data class SGChapterListResponse(
    @Json(name = "data") val data: List<SGChapterItem>
)

@JsonClass(generateAdapter = true)
data class SGPagesData2Dto(
    @Json(name = "path") val path: String,
    @Json(name = "data") val pages: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SGPagesDataDto(
    @Json(name = "chapter") val chapterPage: SGPagesData2Dto
)

@JsonClass(generateAdapter = true)
data class SGPageListResponse(
    @Json(name = "data") val pageList: SGPagesDataDto
)

interface ShinigamiService {
    @GET("v1/manga/list")
    suspend fun getMangaList(
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int = 30,
        @Query("sort") sort: String // "popularity" or "latest"
    ): SGBrowseResponse

    @GET("v1/manga/list")
    suspend fun searchManga(
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int = 40,
        @Query("q") query: String
    ): SGBrowseResponse

    @GET("v1/manga/detail/{mangaId}")
    suspend fun getMangaDetail(
        @Path("mangaId") mangaId: String
    ): SGMangaDetailResponse

    @GET("v1/chapter/{mangaId}/list")
    suspend fun getChapterList(
        @Path("mangaId") mangaId: String,
        @Query("page_size") pageSize: Int = 3000
    ): SGChapterListResponse

    @GET("v1/chapter/detail/{chapterId}")
    suspend fun getChapterPages(
        @Path("chapterId") chapterId: String
    ): SGPageListResponse
}

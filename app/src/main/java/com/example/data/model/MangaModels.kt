package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_manga")
data class SavedManga(
    @PrimaryKey val id: String, // composite id: "source:slug"
    val slug: String,
    val source: String,
    val title: String,
    val thumbnailUrl: String?,
    val author: String?,
    val status: String?,
    val description: String?,
    val genres: String?,
    val savedAt: Long = System.currentTimeMillis(),
    val lastReadChapterId: String? = null,
    val lastReadChapterName: String? = null
)

data class Manga(
    val id: String, // composite id: "source:slug"
    val slug: String,
    val source: String,
    val title: String,
    val thumbnailUrl: String?,
    val author: String? = null,
    val status: String? = null,
    val description: String? = null,
    val genres: String? = null
) {
    fun toSavedManga(): SavedManga {
        return SavedManga(
            id = id,
            slug = slug,
            source = source,
            title = title,
            thumbnailUrl = thumbnailUrl,
            author = author,
            status = status,
            description = description,
            genres = genres
        )
    }
}

data class Chapter(
    val id: String, // e.g., slug of chapter (full path or partial path)
    val name: String,
    val dateUpload: String? = null,
    val number: Float = 0f
)

package com.example.data.local

import androidx.room.*
import com.example.data.model.SavedManga
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMangaDao {
    @Query("SELECT * FROM saved_manga ORDER BY savedAt DESC")
    fun getAllSavedManga(): Flow<List<SavedManga>>

    @Query("SELECT * FROM saved_manga WHERE id = :id LIMIT 1")
    suspend fun getSavedMangaById(id: String): SavedManga?

    @Query("SELECT EXISTS(SELECT 1 FROM saved_manga WHERE id = :id)")
    fun isMangaSavedFlow(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManga(manga: SavedManga)

    @Query("DELETE FROM saved_manga WHERE id = :id")
    suspend fun deleteMangaById(id: String)

    @Query("UPDATE saved_manga SET lastReadChapterId = :chapterId, lastReadChapterName = :chapterName WHERE id = :id")
    suspend fun updateLastReadChapter(id: String, chapterId: String, chapterName: String)
}

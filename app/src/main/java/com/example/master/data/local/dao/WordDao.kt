package com.example.master.data.local.dao

import androidx.room.*
import com.example.master.data.local.entity.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    
    @Query("SELECT * FROM words")
    fun getAllWords(): Flow<List<WordEntity>>
    
    @Query("SELECT * FROM words WHERE lessonId = :lessonId ORDER BY id")
    fun getWordsByLesson(lessonId: Int): Flow<List<WordEntity>>
    
    @Query("SELECT * FROM words WHERE id = :wordId")
    suspend fun getWordById(wordId: Int): WordEntity?
    
    @Query("SELECT * FROM words WHERE category = :category")
    fun getWordsByCategory(category: String): Flow<List<WordEntity>>
    
    @Query("SELECT * FROM words WHERE difficulty = :difficulty")
    fun getWordsByDifficulty(difficulty: Int): Flow<List<WordEntity>>
    
    @Query("SELECT * FROM words WHERE word LIKE '%' || :searchQuery || '%' OR translation LIKE '%' || :searchQuery || '%'")
    fun searchWords(searchQuery: String): Flow<List<WordEntity>>
    
    @Query("SELECT COUNT(*) FROM words")
    suspend fun getTotalWordsCount(): Int
    
    @Query("SELECT COUNT(*) FROM words WHERE lessonId = :lessonId")
    suspend fun getWordsCountByLesson(lessonId: Int): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)
    
    @Update
    suspend fun updateWord(word: WordEntity)
    
    @Delete
    suspend fun deleteWord(word: WordEntity)
    
    @Query("DELETE FROM words")
    suspend fun deleteAllWords()
}

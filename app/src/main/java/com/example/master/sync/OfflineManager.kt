package com.example.master.sync

import android.content.Context
import com.example.master.core.cache.AudioCache
import com.example.master.data.repository.LearningRepository
import com.example.master.network.ApiService
import com.example.master.network.toEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineManager @Inject constructor(
    private val apiService: ApiService,
    private val repository: LearningRepository,
    appContext: Context
) {
    private val audioCache = AudioCache(appContext)

    /**
     * Tải trước toàn bộ bài, từ, bài tập và audio về máy để học offline.
     * Nếu có mạng kém sẽ bỏ qua phần lỗi và vẫn giữ dữ liệu đã tải được.
     */
    suspend fun prefetchAllLessons() {
        val lessons = runCatching { apiService.getLessons() }.getOrElse { return }
        repository.replaceLessons(lessons.map { it.toEntity() })

        lessons.forEach { lesson ->
            prefetchLesson(lesson.id)
        }
    }

    /**
     * Tải trước 1 bài (words + exercises + audio).
     */
    suspend fun prefetchLesson(lessonId: Int) {
        val wordsRemote = runCatching { apiService.getWordsByLesson(lessonId) }.getOrDefault(emptyList())
        val exercisesRemote = runCatching { apiService.getExercisesByLesson(lessonId) }.getOrDefault(emptyList())

        val wordEntities = wordsRemote.map { it.toEntity() }
        repository.replaceWordsForLesson(lessonId, wordEntities)
        repository.replaceExercisesForLesson(lessonId, exercisesRemote.map { it.toEntity() })

        // Cache audio for words that have audioUrl
        wordEntities.mapNotNull { it.audioUrl }.forEach { url ->
            audioCache.cacheIfNeeded(url)
        }
    }
}

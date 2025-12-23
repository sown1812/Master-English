package com.example.master.sync

import com.example.master.data.repository.LearningRepository
import com.example.master.network.ApiService
import com.example.master.network.toEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentSyncManager @Inject constructor(
    private val apiService: ApiService,
    private val repository: LearningRepository
) {
    suspend fun refreshFromServer() {
        val lessons = runCatching { apiService.getLessons() }.getOrElse { return }
        if (lessons.isEmpty()) return
        repository.clearContent()
        repository.replaceLessons(lessons.map { it.toEntity() })

        lessons.forEach { lesson ->
            val words = runCatching { apiService.getWordsByLesson(lesson.id) }
                .getOrDefault(emptyList())
            repository.replaceWordsForLesson(lesson.id, words.map { it.toEntity() })

            val exercises = runCatching { apiService.getExercisesByLesson(lesson.id) }
                .getOrDefault(emptyList())
            repository.replaceExercisesForLesson(lesson.id, exercises.map { it.toEntity() })
        }
    }
}

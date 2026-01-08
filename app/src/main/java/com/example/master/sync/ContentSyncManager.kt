package com.example.master.sync

import com.example.master.BuildConfig
import com.example.master.data.remote.RealtimeDatabaseService
import com.example.master.data.repository.LearningRepository
import com.example.master.network.toEntity
import com.example.master.network.toRemote
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentSyncManager @Inject constructor(
    private val realtimeDatabaseService: RealtimeDatabaseService,
    private val repository: LearningRepository
) {
    suspend fun refreshFromServer() {
        val hasContent = runCatching { realtimeDatabaseService.hasContent() }.getOrDefault(false)
        if (!hasContent) {
            if (BuildConfig.DEBUG) {
                seedFromLocal()
            }
            return
        }

        val lessons = runCatching { realtimeDatabaseService.fetchLessons() }.getOrDefault(emptyList())
        if (lessons.isEmpty()) return

        repository.clearContent()
        repository.replaceLessons(lessons.map { it.toEntity() })

        lessons.forEach { lesson ->
            val words = runCatching { realtimeDatabaseService.fetchWords(lesson.id) }
                .getOrDefault(emptyList())
            repository.replaceWordsForLesson(lesson.id, words.map { it.toEntity() })

            val exercises = runCatching { realtimeDatabaseService.fetchExercises(lesson.id) }
                .getOrDefault(emptyList())
            repository.replaceExercisesForLesson(lesson.id, exercises.map { it.toEntity() })
        }
    }

    private suspend fun seedFromLocal() {
        val lessons = repository.getAllLessonsList()
        if (lessons.isEmpty()) return
        val words = repository.getAllWordsList()
        val exercises = repository.getAllExercisesList()

        realtimeDatabaseService.seedContent(
            lessons = lessons.map { it.toRemote() },
            words = words.map { it.toRemote() },
            exercises = exercises.map { it.toRemote() }
        )
    }
}

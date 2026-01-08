package com.example.master.data.remote

import com.example.master.network.AchievementRemote
import com.example.master.network.ExerciseRemote
import com.example.master.network.LessonRemote
import com.example.master.network.ProgressRemote
import com.example.master.network.UserRemote
import com.example.master.network.WordRemote
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

data class GameStateSnapshot(
    val boosters: Set<String> = emptySet(),
    val quests: Set<String> = emptySet()
)

@Singleton
class RealtimeDatabaseService @Inject constructor(
    private val database: FirebaseDatabase,
    private val gson: Gson
) {
    private val root = database.reference

    suspend fun hasContent(): Boolean {
        val snapshot = root.child("content/lessons").get().await()
        return snapshot.exists() && snapshot.childrenCount > 0
    }

    suspend fun fetchLessons(): List<LessonRemote> {
        val type = object : TypeToken<Map<String, LessonRemote>>() {}.type
        return readMap<LessonRemote>("content/lessons", type).values.sortedBy { it.id }
    }

    suspend fun fetchWords(lessonId: Int): List<WordRemote> {
        val type = object : TypeToken<Map<String, WordRemote>>() {}.type
        return readMap<WordRemote>("content/words/$lessonId", type).values.sortedBy { it.id }
    }

    suspend fun fetchExercises(lessonId: Int): List<ExerciseRemote> {
        val type = object : TypeToken<Map<String, ExerciseRemote>>() {}.type
        return readMap<ExerciseRemote>("content/exercises/$lessonId", type).values.sortedBy { it.order }
    }

    suspend fun seedContent(
        lessons: List<LessonRemote>,
        words: List<WordRemote>,
        exercises: List<ExerciseRemote>
    ) {
        val updates = mutableMapOf<String, Any?>()
        lessons.forEach { lesson ->
            updates["content/lessons/${lesson.id}"] = lesson
        }
        words.groupBy { it.lessonId }.forEach { (lessonId, items) ->
            items.forEach { word ->
                updates["content/words/$lessonId/${word.id}"] = word
            }
        }
        exercises.groupBy { it.lessonId }.forEach { (lessonId, items) ->
            items.forEach { exercise ->
                updates["content/exercises/$lessonId/${exercise.id}"] = exercise
            }
        }
        if (updates.isNotEmpty()) {
            root.updateChildren(updates).await()
        }
    }

    suspend fun getUserProfile(userId: String): UserRemote? {
        val type = object : TypeToken<UserRemote>() {}.type
        return readObject<UserRemote>("users/$userId/profile", type)
    }

    suspend fun saveUserProfile(userId: String, profile: UserRemote) {
        root.child("users").child(userId).child("profile").setValue(profile).await()
    }

    suspend fun getUserProgress(userId: String): List<ProgressRemote> {
        val type = object : TypeToken<Map<String, ProgressRemote>>() {}.type
        return readMap<ProgressRemote>("users/$userId/progress", type).values.sortedBy { it.lessonId }
    }

    suspend fun saveUserProgress(userId: String, items: List<ProgressRemote>) {
        val payload = items.associateBy { progressKey(it.lessonId) }
        root.child("users").child(userId).child("progress").setValue(payload).await()
    }

    suspend fun getUserAchievements(userId: String): List<AchievementRemote> {
        val type = object : TypeToken<Map<String, AchievementRemote>>() {}.type
        return readMap<AchievementRemote>("users/$userId/achievements", type).values.sortedBy { it.achievementType }
    }

    suspend fun saveUserAchievements(userId: String, items: List<AchievementRemote>) {
        val payload = items.associateBy { it.achievementType }
        root.child("users").child(userId).child("achievements").setValue(payload).await()
    }

    suspend fun getGameState(userId: String): GameStateSnapshot {
        val boosters = readBooleanMap("users/$userId/gamestate/boosters")
            .filterValues { it }
            .keys
        val quests = readBooleanMap("users/$userId/gamestate/quests")
            .filterValues { it }
            .keys
        return GameStateSnapshot(boosters = boosters, quests = quests)
    }

    suspend fun setBoosterOwned(userId: String, boosterKey: String, owned: Boolean) {
        root.child("users").child(userId).child("gamestate").child("boosters")
            .child(boosterKey).setValue(owned).await()
    }

    suspend fun setQuestClaimed(userId: String, questKey: String, claimed: Boolean) {
        root.child("users").child(userId).child("gamestate").child("quests")
            .child(questKey).setValue(claimed).await()
    }

    private fun progressKey(lessonId: Int): String = "lesson_$lessonId"

    private suspend fun <T> readObject(path: String, type: java.lang.reflect.Type): T? {
        val snapshot = root.child(path).get().await()
        if (!snapshot.exists()) return null
        val json = gson.toJson(snapshot.value)
        return runCatching { gson.fromJson<T>(json, type) }.getOrNull()
    }

    private suspend fun <T> readMap(path: String, type: java.lang.reflect.Type): Map<String, T> {
        val snapshot = root.child(path).get().await()
        if (!snapshot.exists()) return emptyMap()
        val json = gson.toJson(snapshot.value)
        return runCatching { gson.fromJson<Map<String, T>>(json, type) }.getOrDefault(emptyMap())
    }

    private suspend fun readBooleanMap(path: String): Map<String, Boolean> {
        val type = object : TypeToken<Map<String, Boolean>>() {}.type
        return readMap<Boolean>(path, type)
    }
}

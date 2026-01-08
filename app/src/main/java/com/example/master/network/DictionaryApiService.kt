package com.example.master.network

import retrofit2.http.GET
import retrofit2.http.Path

interface DictionaryApiService {
    @GET("entries/en/{word}")
    suspend fun getWordDefinition(@Path("word") word: String): List<DictionaryEntryRemote>
}

data class DictionaryEntryRemote(
    val word: String,
    val phonetic: String? = null,
    val phonetics: List<PhoneticRemote>? = null
)

data class PhoneticRemote(
    val text: String? = null,
    val audio: String? = null
)

package com.example.master.core.cache

import android.content.Context
import java.io.File
import java.security.MessageDigest
import okhttp3.OkHttpClient
import okhttp3.Request

class AudioCache(context: Context) {
    private val appContext = context.applicationContext
    private val cacheDir: File = File(appContext.cacheDir, "audio_cache").apply { mkdirs() }
    private val client = OkHttpClient()

    fun getCachedPath(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val file = File(cacheDir, hash(url))
        return if (file.exists()) file.absolutePath else null
    }

    fun cacheIfNeeded(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val file = File(cacheDir, hash(url))
        if (file.exists()) return file.absolutePath

        return runCatching {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body ?: return null
                file.outputStream().use { out -> body.byteStream().copyTo(out) }
                file.absolutePath
            }
        }.getOrNull()
    }

    private fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) } + ".mp3"
    }
}

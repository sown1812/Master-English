package com.example.master.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.math.min
import kotlin.random.Random

class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val baseDelayMs: Long = 200
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var lastException: IOException? = null
        while (attempt <= maxRetries) {
            try {
                return chain.proceed(chain.request())
            } catch (ioe: IOException) {
                lastException = ioe
                if (attempt == maxRetries) break
                val delay = computeDelay(attempt)
                Thread.sleep(delay)
            }
            attempt++
        }
        throw lastException ?: IOException("Unknown network error")
    }

    private fun computeDelay(attempt: Int): Long {
        val exp = baseDelayMs * (1 shl attempt)
        val jitter = Random.nextLong(0, baseDelayMs)
        return min(exp + jitter, 5_000L)
    }
}

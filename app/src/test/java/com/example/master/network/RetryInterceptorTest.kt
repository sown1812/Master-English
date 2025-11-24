package com.example.master.network

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class RetryInterceptorTest {

    @Test
    fun `retries until success within max retries`() {
        val interceptor = RetryInterceptor(maxRetries = 2, baseDelayMs = 1)
        val chain = FailingChain(failuresBeforeSuccess = 2)

        val response = interceptor.intercept(chain)

        assertEquals(3, chain.attempts)
        assertEquals(200, response.code)
    }

    private class FailingChain(private var failuresBeforeSuccess: Int) : Interceptor.Chain {
        var attempts = 0

        override fun request(): Request {
            return Request.Builder()
                .url("http://example.com")
                .build()
        }

        override fun proceed(request: Request): Response {
            attempts++
            if (failuresBeforeSuccess > 0) {
                failuresBeforeSuccess--
                throw IOException("Simulated failure")
            }
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }

        override fun call() = throw NotImplementedError()
        override fun connectTimeoutMillis() = 0
        override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit?) = this
        override fun readTimeoutMillis() = 0
        override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit?) = this
        override fun writeTimeoutMillis() = 0
        override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit?) = this
        override fun connection() = null
    }
}

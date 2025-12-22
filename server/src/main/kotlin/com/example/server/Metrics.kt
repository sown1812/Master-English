package com.example.server

import java.util.concurrent.atomic.AtomicLong

object MetricsRegistry {
    private val startMs = System.currentTimeMillis()
    val totalRequests = AtomicLong(0)
    val rateLimitedRequests = AtomicLong(0)
    val payloadTooLargeRequests = AtomicLong(0)
    val dbReady = AtomicLong(0)

    fun uptimeSeconds(nowMs: Long = System.currentTimeMillis()): Long =
        ((nowMs - startMs) / 1000L).coerceAtLeast(0)
}

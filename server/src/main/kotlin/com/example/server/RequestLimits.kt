package com.example.server

import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.request.contentLength
import io.ktor.server.request.path
import io.ktor.http.HttpHeaders
import java.util.concurrent.ConcurrentHashMap

private const val DEFAULT_MAX_REQUEST_BYTES = 512 * 1024L
private const val DEFAULT_RATE_WINDOW_MS = 60_000L
private const val DEFAULT_RATE_MAX_REQUESTS = 120
private val DEFAULT_RATE_ALLOW_PATHS = setOf("/health", "/metrics")
private val DEFAULT_READY_ALLOW_PATHS = setOf("/ready")
private const val DEFAULT_RATE_ALLOW_CIDRS = ""

class PayloadTooLargeException : RuntimeException("Request too large")
class RateLimitExceededException : RuntimeException("Rate limit exceeded")

private class FixedWindowRateLimiter(
    private val windowMs: Long,
    private val maxRequests: Int
) {
    private data class Window(var startMs: Long, var count: Int)

    private val windows = ConcurrentHashMap<String, Window>()

    fun allow(key: String, now: Long): Boolean {
        val window = windows.compute(key) { _, existing ->
            if (existing == null || now - existing.startMs >= windowMs) {
                Window(now, 1)
            } else {
                existing.count += 1
                existing
            }
        } ?: return true

        return window.count <= maxRequests
    }
}

private val RequestLimitsPlugin = createApplicationPlugin("RequestLimits") {
    val maxBytes = System.getenv("MAX_REQUEST_BYTES")?.toLongOrNull() ?: DEFAULT_MAX_REQUEST_BYTES
    val windowMs = System.getenv("RATE_LIMIT_WINDOW_MS")?.toLongOrNull() ?: DEFAULT_RATE_WINDOW_MS
    val maxRequests = System.getenv("RATE_LIMIT_MAX_REQUESTS")?.toIntOrNull() ?: DEFAULT_RATE_MAX_REQUESTS
    val allowPaths = System.getenv("RATE_LIMIT_ALLOW_PATHS")
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.toSet()
        ?: DEFAULT_RATE_ALLOW_PATHS
    val readyPaths = System.getenv("RATE_LIMIT_READY_ALLOW_PATHS")
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.toSet()
        ?: DEFAULT_READY_ALLOW_PATHS
    val allowCidrs = parseCidrList(
        System.getenv("RATE_LIMIT_ALLOW_CIDRS") ?: DEFAULT_RATE_ALLOW_CIDRS
    )
    val rateLimiter = FixedWindowRateLimiter(windowMs, maxRequests)

    onCall { call ->
        MetricsRegistry.totalRequests.incrementAndGet()
        val path = call.request.path()
        val forwardedFor = parseForwardedFor(call.request.headers[HttpHeaders.Forwarded])
            ?: call.request.headers[HttpHeaders.XForwardedFor]
                ?.split(",")
                ?.firstOrNull()
                ?.trim()
        val realIp = call.request.headers["X-Real-IP"]
        val ipForCidr = normalizeIpv4(forwardedFor ?: realIp)
        val allowlisted = if (allowCidrs.isNotEmpty() && ipForCidr != null) {
            isAllowedByCidr(ipForCidr, allowCidrs)
        } else {
            false
        }
        val isAllowlistedPath = allowPaths.contains(path) && (allowCidrs.isEmpty() || allowlisted)
        val isReadyAllowlisted = readyPaths.contains(path) && allowlisted
        if (isAllowlistedPath || isReadyAllowlisted) return@onCall

        val contentLength = call.request.contentLength()
        if (contentLength != null && contentLength > maxBytes) {
            MetricsRegistry.payloadTooLargeRequests.incrementAndGet()
            throw PayloadTooLargeException()
        }

        val key = forwardedFor ?: realIp ?: "unknown"
        if (!rateLimiter.allow(key, System.currentTimeMillis())) {
            MetricsRegistry.rateLimitedRequests.incrementAndGet()
            throw RateLimitExceededException()
        }
    }
}

fun Application.configureRequestLimits() {
    install(RequestLimitsPlugin)
}

internal fun parseForwardedFor(headerValue: String?): String? {
    if (headerValue.isNullOrBlank()) return null
    val firstEntry = headerValue.split(",").firstOrNull()?.trim().orEmpty()
    if (firstEntry.isEmpty()) return null
    val parts = firstEntry.split(";").map { it.trim() }
    val forPart = parts.firstOrNull { it.startsWith("for=", ignoreCase = true) } ?: return null
    val raw = forPart.substringAfter("=", "")
    if (raw.isEmpty()) return null
    val unquoted = raw.trim().trim('"')
    val stripped = unquoted.removePrefix("[").removeSuffix("]")
    return stripped.ifEmpty { null }
}

internal data class CidrBlock(val network: Int, val maskBits: Int)

internal fun parseCidrList(value: String?): List<CidrBlock> {
    if (value.isNullOrBlank()) return emptyList()
    return value.split(",")
        .mapNotNull { entry ->
            val trimmed = entry.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            val parts = trimmed.split("/")
            val ip = parts[0].trim()
            val maskBits = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 32
            val ipInt = ipv4ToInt(ip) ?: return@mapNotNull null
            if (maskBits !in 0..32) return@mapNotNull null
            CidrBlock(ipInt, maskBits)
        }
}

internal fun isAllowedByCidr(ip: String, cidrs: List<CidrBlock>): Boolean {
    val ipInt = ipv4ToInt(ip) ?: return false
    return cidrs.any { block ->
        val mask = if (block.maskBits == 0) 0 else -1 shl (32 - block.maskBits)
        (ipInt and mask) == (block.network and mask)
    }
}

internal fun normalizeIpv4(ip: String?): String? {
    if (ip.isNullOrBlank()) return null
    val trimmed = ip.trim()
    if (trimmed.count { it == '.' } == 3 && trimmed.contains(":")) {
        return trimmed.substringBeforeLast(":")
    }
    return trimmed
}

private fun ipv4ToInt(ip: String): Int? {
    val parts = ip.split(".")
    if (parts.size != 4) return null
    val bytes = parts.map { it.toIntOrNull() ?: return null }
    if (bytes.any { it !in 0..255 }) return null
    return (bytes[0] shl 24) or (bytes[1] shl 16) or (bytes[2] shl 8) or bytes[3]
}

package com.example.server.routes

import com.example.server.MetricsRegistry
import com.example.server.dbPing
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.healthRoutes() {
    route("/") {
        get("health") {
            call.respond(mapOf("status" to "ok"))
        }
        get("ready") {
            val ok = dbPing()
            MetricsRegistry.dbReady.set(if (ok) 1 else 0)
            val status = if (ok) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
            val body = if (ok) "ok" else "db_unavailable"
            call.respond(status, mapOf("status" to body))
        }
        get("metrics") {
            val uptime = MetricsRegistry.uptimeSeconds()
            val payload = buildString {
                appendLine("# HELP app_up App availability")
                appendLine("# TYPE app_up gauge")
                appendLine("app_up 1")
                appendLine("# HELP app_uptime_seconds Process uptime in seconds")
                appendLine("# TYPE app_uptime_seconds gauge")
                appendLine("app_uptime_seconds $uptime")
                appendLine("# HELP app_requests_total Total requests seen")
                appendLine("# TYPE app_requests_total counter")
                appendLine("app_requests_total ${MetricsRegistry.totalRequests.get()}")
                appendLine("# HELP app_requests_rate_limited_total Requests rejected by rate limiting")
                appendLine("# TYPE app_requests_rate_limited_total counter")
                appendLine("app_requests_rate_limited_total ${MetricsRegistry.rateLimitedRequests.get()}")
                appendLine("# HELP app_requests_payload_too_large_total Requests rejected due to size")
                appendLine("# TYPE app_requests_payload_too_large_total counter")
                appendLine("app_requests_payload_too_large_total ${MetricsRegistry.payloadTooLargeRequests.get()}")
                appendLine("# HELP app_db_ready Database readiness (1=ready)")
                appendLine("# TYPE app_db_ready gauge")
                appendLine("app_db_ready ${MetricsRegistry.dbReady.get()}")
            }
            call.respondText(payload, ContentType.Text.Plain)
        }
    }
}

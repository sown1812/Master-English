package com.example.server.auth

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.Principal
import io.ktor.server.auth.bearer
import io.ktor.server.response.*
import java.io.ByteArrayInputStream
import java.io.FileInputStream

data class FirebasePrincipal(
    val uid: String,
    val email: String? = null,
    val name: String? = null
) : Principal

private object FirebaseAdmin {
    @Synchronized
    fun init() {
        if (FirebaseApp.getApps().isNotEmpty()) return

        val credentialsJson = System.getenv("FIREBASE_CREDENTIALS_JSON")
        val credentialsFile = System.getenv("FIREBASE_CREDENTIALS_FILE")

        val stream = when {
            !credentialsJson.isNullOrBlank() -> ByteArrayInputStream(credentialsJson.toByteArray())
            !credentialsFile.isNullOrBlank() -> FileInputStream(credentialsFile)
            else -> throw IllegalStateException(
                "Firebase credentials not configured. Set FIREBASE_CREDENTIALS_JSON or FIREBASE_CREDENTIALS_FILE."
            )
        }

        stream.use {
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(it))
                .build()
            FirebaseApp.initializeApp(options)
        }
    }
}

fun Application.configureFirebaseAuth() {
    val disabled = System.getenv("FIREBASE_AUTH_DISABLED")?.equals("true", ignoreCase = true) == true
    if (disabled) {
        install(Authentication) {
            bearer("firebaseAuth") {
                authenticate { tokenCredential ->
                    val token = tokenCredential.token.takeIf { it.isNotBlank() }
                    token?.let { FirebasePrincipal(uid = it) }
                }
            }
        }
    } else {
        FirebaseAdmin.init()
        install(Authentication) {
            bearer("firebaseAuth") {
                authenticate { tokenCredential ->
                    val token = try {
                        FirebaseAuth.getInstance().verifyIdToken(tokenCredential.token, true)
                    } catch (_: Exception) {
                        null
                    }
                    token?.let { FirebasePrincipal(uid = it.uid, email = it.email, name = it.name) }
                }
            }
        }
    }
}

suspend fun ApplicationCall.requireFirebaseUser(): FirebasePrincipal? {
    val principal = principal<FirebasePrincipal>()
    if (principal == null) {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing or invalid Firebase token"))
    }
    return principal
}

suspend fun ApplicationCall.ensureUser(userId: String): Boolean {
    val principal = requireFirebaseUser() ?: return false
    if (principal.uid != userId) {
        respond(HttpStatusCode.Forbidden, mapOf("error" to "Forbidden"))
        return false
    }
    return true
}

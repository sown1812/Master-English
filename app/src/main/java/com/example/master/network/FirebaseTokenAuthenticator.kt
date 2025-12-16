package com.example.master.network

import com.example.master.auth.AuthManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class FirebaseTokenAuthenticator(
    private val authManager: AuthManager
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val token = runBlocking { authManager.getIdToken(forceRefresh = true) } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}


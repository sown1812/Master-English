package com.example.master.network

import com.example.master.BuildConfig
import com.example.master.auth.AuthManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {

    fun createApiService(authManager: AuthManager): ApiService {
        val logger = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val reqBuilder = chain.request().newBuilder()
                // Use cached token or fetch synchronously if missing (avoids 401)
                val token = authManager.getCachedIdToken() ?: kotlinx.coroutines.runBlocking {
                    authManager.getIdToken(forceRefresh = false)
                }
                token?.let {
                    reqBuilder.addHeader("Authorization", "Bearer $it")
                }
                chain.proceed(reqBuilder.build())
            }
            .authenticator(FirebaseTokenAuthenticator(authManager))
            .addInterceptor(RetryInterceptor())
            .addInterceptor(logger)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun createDictionaryService(): DictionaryApiService {
        val logger = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)
        val client = OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.dictionaryapi.dev/api/v2/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DictionaryApiService::class.java)
    }
}

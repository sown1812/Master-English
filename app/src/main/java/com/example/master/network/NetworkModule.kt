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
                authManager.getCurrentUserId()?.let { uid ->
                    reqBuilder.addHeader("Authorization", "Bearer $uid")
                }
                chain.proceed(reqBuilder.build())
            }
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
}

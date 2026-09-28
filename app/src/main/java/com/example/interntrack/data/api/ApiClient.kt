package com.example.interntrack.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // Render deployed backend proxy URL
    private const val BASE_URL = "https://interntrack-ukln.onrender.com/"

    val apiService: InternshipApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(InternshipApiService::class.java)
    }
}

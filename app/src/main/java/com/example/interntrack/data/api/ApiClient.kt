package com.example.interntrack.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // 127.0.0.1 works on physical devices with ADB reverse port forwarding (adb reverse tcp:5000 tcp:5000) and emulators
    private const val BASE_URL = "http://127.0.0.1:5000/"

    val apiService: InternshipApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(InternshipApiService::class.java)
    }
}

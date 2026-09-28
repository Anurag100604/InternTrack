package com.example.interntrack.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface InternshipApiService {
    @GET("api/internships")
    suspend fun getInternships(
        @Query("what") what: String? = null,
        @Query("where") where: String? = null,
        @Query("page") page: Int = 1
    ): AdzunaApiResponse
}

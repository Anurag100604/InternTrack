package com.example.interntrack.data.api

import com.example.interntrack.data.Opportunity

data class AdzunaApiResponse(
    val success: Boolean = false,
    val count: Int = 0,
    val page: Int = 1,
    val internships: List<Opportunity> = emptyList()
)

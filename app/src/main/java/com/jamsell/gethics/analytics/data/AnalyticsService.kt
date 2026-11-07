package com.jamsell.gethics.analytics.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AnalyticsService {

    /** US20. Reporte del ganado entre dos fechas (yyyy-MM-dd, las dos obligatorias). 400 si el rango es invalido. */
    @GET("api/v1/reports")
    suspend fun getReport(
        @Query("ownerId") ownerId: String,
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<LivestockReportResponse>
}

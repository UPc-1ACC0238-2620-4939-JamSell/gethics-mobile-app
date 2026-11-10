package com.jamsell.gethics.analytics.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

interface AnalyticsService {

    /** US20. Reporte del ganado entre dos fechas (yyyy-MM-dd, las dos obligatorias). 400 si el rango es invalido. */
    @GET("api/v1/reports")
    suspend fun getReport(
        @Query("ownerId") ownerId: String,
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<LivestockReportResponse>

    /**
     * US39. Exporta el reporte del rango como archivo CSV (Content-Disposition: attachment).
     * 400 si el rango es invalido; 404 con message si no hay informacion en el periodo.
     */
    @Streaming
    @GET("api/v1/reports/export")
    suspend fun exportReport(
        @Query("ownerId") ownerId: String,
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<ResponseBody>
}

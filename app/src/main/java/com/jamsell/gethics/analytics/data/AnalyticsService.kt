package com.jamsell.gethics.analytics.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

interface AnalyticsService {

    /**
     * US20 + US49. Reporte del ganado entre dos fechas (yyyy-MM-dd, las dos obligatorias). 400 si el rango es
     * invalido. type (INCOME | EXPENSE) y category son opcionales y se combinan con AND; Retrofit omite de la URL
     * los que son null, y el backend (no Android) hace el filtrado.
     */
    @GET("api/v1/reports")
    suspend fun getReport(
        @Query("ownerId") ownerId: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("type") type: String?,
        @Query("category") category: String?
    ): Response<LivestockReportResponse>

    /**
     * US39 + US49. Exporta el reporte como archivo CSV (Content-Disposition: attachment) con exactamente los mismos
     * parametros que getReport. 400 si el rango o el type son invalidos; 404 con message si no hay informacion.
     */
    @Streaming
    @GET("api/v1/reports/export")
    suspend fun exportReport(
        @Query("ownerId") ownerId: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("type") type: String?,
        @Query("category") category: String?
    ): Response<ResponseBody>
}

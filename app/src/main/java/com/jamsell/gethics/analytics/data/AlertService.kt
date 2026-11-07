package com.jamsell.gethics.analytics.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * US21. Va aparte de AnalyticsService (US20) para no pisar ese archivo mientras feat/app-reportes no este en develop;
 * mismo bounded context (analytics) y mismo patron Service -> Repository.
 */
interface AlertService {

    /** Alertas por tendencia del ganadero, de la mas reciente a la mas antigua. 200 (lista vacia con message); 400 si ownerId falta o no es UUID. */
    @GET("api/v1/alerts")
    suspend fun getAlerts(@Query("ownerId") ownerId: String): Response<AlertListResponse>
}

package com.jamsell.gethics.sanitary.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SanitaryService {

    /** US11. Responde 201 Created; el body no se usa todavia (el historial es US14). */
    @POST("api/v1/animals/{animalId}/sanitary-events")
    suspend fun registerEvent(
        @Path("animalId") animalId: String,
        @Body request: RegisterSanitaryEventRequest
    ): Response<Unit>

    /** US12. Eventos SCHEDULED del mes; month va de 1 a 12. */
    @GET("api/v1/sanitary-calendar")
    suspend fun getCalendar(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<SanitaryCalendarResponse>

    /** US13. Programa un evento (SCHEDULED). 201; 400 si la fecha programada es anterior a hoy. */
    @POST("api/v1/animals/{animalId}/sanitary-events/scheduled")
    suspend fun scheduleEvent(
        @Path("animalId") animalId: String,
        @Body request: ScheduleSanitaryEventRequest
    ): Response<Unit>

    /** US13. Registra como aplicado el mismo evento programado (SCHEDULED -> COMPLETED). 200; 400 fecha futura; 404; 409 si no esta SCHEDULED. */
    @POST("api/v1/animals/{animalId}/sanitary-events/{eventId}/complete")
    suspend fun completeEvent(
        @Path("animalId") animalId: String,
        @Path("eventId") eventId: String,
        @Body request: CompleteSanitaryEventRequest
    ): Response<Unit>

    /** US14. Historial completo del animal; un UUID sin eventos (o inexistente) responde 200 con "Sin registros.". */
    @GET("api/v1/animals/{animalId}/clinical-history")
    suspend fun getClinicalHistory(@Path("animalId") animalId: String): Response<ClinicalHistoryResponse>
}

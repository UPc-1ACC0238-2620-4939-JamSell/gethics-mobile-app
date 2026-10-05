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
}

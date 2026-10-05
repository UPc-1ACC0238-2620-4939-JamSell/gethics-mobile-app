package com.jamsell.gethics.sanitary.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface SanitaryService {

    /** US11. Responde 201 Created; el body no se usa todavia (el historial es US14). */
    @POST("api/v1/animals/{animalId}/sanitary-events")
    suspend fun registerEvent(
        @Path("animalId") animalId: String,
        @Body request: RegisterSanitaryEventRequest
    ): Response<Unit>
}

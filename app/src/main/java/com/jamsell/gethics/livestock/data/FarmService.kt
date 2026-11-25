package com.jamsell.gethics.livestock.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface FarmService {

    /** Ficha de una granja (US-43: granja asignada de un animal). 404 si no existe, 400 si el id no es un UUID. */
    @GET("api/v1/farms/{farmId}")
    suspend fun getFarm(@Path("farmId") farmId: String): Response<FarmResponse>
}

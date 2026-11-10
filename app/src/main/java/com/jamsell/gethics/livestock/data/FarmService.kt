package com.jamsell.gethics.livestock.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface FarmService {

    /** Granjas del dueno por nombre ascendente; 400 si ownerId falta o no es un UUID. */
    @GET("api/v1/farms")
    suspend fun getFarms(@Query("ownerId") ownerId: String): Response<FarmListResponse>

    /** US-32. 200 con la granja; 400 por datos invalidos, 404 si no existe y 409 si el nombre ya lo usa otra granja del dueno. */
    @PUT("api/v1/farms/{farmId}")
    suspend fun updateFarm(@Path("farmId") farmId: String, @Body request: UpdateFarmRequest): Response<FarmResponse>
}

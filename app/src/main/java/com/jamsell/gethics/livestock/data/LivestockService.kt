package com.jamsell.gethics.livestock.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LivestockService {

    /** US05. Responde 201 con el animal creado; 400 por datos invalidos y 409 si el arete ya existe. */
    @POST("api/v1/animals")
    suspend fun registerAnimal(@Body request: RegisterAnimalRequest): Response<AnimalResponse>

    /**
     * US06. search busca por arete, nombre o raza; status es ACTIVE, SOLD, DECEASED o INACTIVE (null = ACTIVE).
     * Retrofit omite los parametros null. 400 si el status es desconocido o search supera 100 caracteres.
     */
    @GET("api/v1/animals")
    suspend fun getAnimals(
        @Query("search") search: String?,
        @Query("status") status: String?
    ): Response<AnimalListResponse>

    // TODO: GET animal por id (ficha del animal, US07)
}

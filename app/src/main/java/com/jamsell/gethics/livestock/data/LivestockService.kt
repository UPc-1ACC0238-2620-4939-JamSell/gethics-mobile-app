package com.jamsell.gethics.livestock.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
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

    /** US07. Ficha del animal; 404 si no existe. */
    @GET("api/v1/animals/{animalId}")
    suspend fun getAnimal(@Path("animalId") animalId: String): Response<AnimalResponse>

    /** US07. Guarda los cambios del animal y lo devuelve; 400 por datos invalidos y 404 si no existe. */
    @PUT("api/v1/animals/{animalId}")
    suspend fun updateAnimal(
        @Path("animalId") animalId: String,
        @Body request: UpdateAnimalRequest
    ): Response<AnimalResponse>
}

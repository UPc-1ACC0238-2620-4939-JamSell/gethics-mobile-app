package com.jamsell.gethics.livestock.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LivestockService {

    /** US05. Responde 201 con el animal creado; 400 por datos invalidos y 409 si el arete ya existe. */
    @POST("api/v1/animals")
    suspend fun registerAnimal(@Body request: RegisterAnimalRequest): Response<AnimalResponse>

    // TODO: GET animales (inventario, US06), GET animal por id
}

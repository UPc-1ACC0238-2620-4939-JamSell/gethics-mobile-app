package com.jamsell.gethics.livestock.data.repository

import com.jamsell.gethics.livestock.data.LivestockService
import com.jamsell.gethics.livestock.data.RegisterAnimalRequest
import com.jamsell.gethics.livestock.data.local.AnimalDao
import com.jamsell.gethics.livestock.data.local.toEntity
import com.jamsell.gethics.livestock.data.toAnimal
import com.jamsell.gethics.livestock.domain.model.Animal
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.remote.errorMessage
import java.io.IOException

const val CONNECTION_ERROR = "No se pudo conectar con el servidor"

/**
 * Patron offline-first (igual que HeroRepository de SuperHero con su Dao):
 * 1) intentar traer del backend y guardar en Room, 2) si no hay red, leer de Room.
 */
class LivestockRepository(
    private val service: LivestockService,
    private val dao: AnimalDao
) {
    /**
     * US05. Registra el animal en el backend y guarda en Room la respuesta (con el arete normalizado y el QR del servidor).
     * Sin conexion devuelve error: aun no hay cola de sincronizacion para registros hechos offline.
     * localPhotoUri se guarda solo en Room; el backend todavia no recibe fotos.
     */
    suspend fun registerAnimal(
        tag: String,
        breed: String,
        birthDate: String,
        weightKg: Double?,
        localPhotoUri: String?
    ): Resource<Animal> =
        try {
            val response = service.registerAnimal(RegisterAnimalRequest(tag, breed, birthDate, weightKg))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val animal = body.toAnimal(localPhotoUri)
                dao.insert(animal.toEntity())
                Resource.Success(animal)
            } else {
                Resource.Error(response.errorMessage())
            }
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    // TODO: suspend fun getAnimals(): Resource<List<Animal>>
}

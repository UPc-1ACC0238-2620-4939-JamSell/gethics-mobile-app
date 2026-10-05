package com.jamsell.gethics.livestock.data.repository

import com.jamsell.gethics.livestock.data.LivestockService
import com.jamsell.gethics.livestock.data.RegisterAnimalRequest
import com.jamsell.gethics.livestock.data.UpdateAnimalRequest
import com.jamsell.gethics.livestock.data.local.AnimalDao
import com.jamsell.gethics.livestock.data.local.toAnimal
import com.jamsell.gethics.livestock.data.local.toEntity
import com.jamsell.gethics.livestock.data.toAnimal
import com.jamsell.gethics.livestock.domain.model.Animal
import com.jamsell.gethics.livestock.domain.model.AnimalStatus
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.remote.errorMessage
import java.io.IOException
import java.util.Locale

const val CONNECTION_ERROR = "No se pudo conectar con el servidor"

private const val LIKE_ESCAPE = '!'

/**
 * Patron LIKE "contiene", en minusculas y con los comodines del usuario (%, _) y el escape tomados literalmente.
 * Mismo criterio que el backend; sin busqueda devuelve "%" (todo).
 */
fun searchPattern(search: String?): String {
    if (search.isNullOrBlank()) return "%"
    val escaped = search.trim().lowercase(Locale.ROOT)
        .replace(LIKE_ESCAPE.toString(), "$LIKE_ESCAPE$LIKE_ESCAPE")
        .replace("%", "$LIKE_ESCAPE%")
        .replace("_", "${LIKE_ESCAPE}_")
    return "%$escaped%"
}

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

    /**
     * US06. Inventario del backend para ese estado, con busqueda opcional por arete, nombre o raza.
     * - Exito: se guarda en Room (sin busqueda reemplaza el cache del estado; con busqueda solo agrega/actualiza) y se
     *   conservan las fotos locales, porque el backend aun no devuelve photoUrl.
     * - Sin conexion: Resource.Error con el mensaje de conexion Y los animales guardados que cumplen el filtro en data.
     * - Otro error del backend: Resource.Error con su message y sin data.
     */
    suspend fun getAnimals(search: String?, status: AnimalStatus): Resource<List<Animal>> {
        val criterion = search?.trim()?.takeIf { it.isNotEmpty() }
        return try {
            val response = service.getAnimals(criterion, status.name)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val localPhotos = dao.fetchAll().associate { it.id to it.photoUrl }
                val animals = body.animals.map { it.toAnimal(localPhotos[it.id]) }
                if (criterion == null) {
                    dao.replaceByStatus(status.name, animals.map { it.toEntity() })
                } else {
                    dao.insertAll(animals.map { it.toEntity() })
                }
                Resource.Success(animals)
            } else {
                Resource.Error(response.errorMessage())
            }
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR, dao.search(searchPattern(criterion), status.name).map { it.toAnimal() })
        }
    }

    /**
     * US07. Ficha del animal desde el backend, guardada en Room (conserva la foto local).
     * Sin conexion: Resource.Error con el mensaje de conexion y, si el animal esta guardado, ese animal en data.
     * Otro error del backend (404, ...): Resource.Error con su message y sin data.
     */
    suspend fun getAnimal(id: String): Resource<Animal> =
        try {
            val response = service.getAnimal(id)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val animal = body.toAnimal(dao.fetchById(id)?.photoUrl)
                dao.insert(animal.toEntity())
                Resource.Success(animal)
            } else {
                Resource.Error(response.errorMessage())
            }
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR, dao.fetchById(id)?.toAnimal())
        }

    /**
     * US07, escenario 1. Guarda los cambios y actualiza Room con la respuesta. El PUT reemplaza los campos editables, asi
     * que se reenvian los que el formulario no pide: peso y granja actuales y la foto si es una URL del backend.
     * Sin conexion devuelve error: aun no hay cola de sincronizacion para ediciones hechas offline.
     */
    suspend fun updateAnimal(
        current: Animal,
        name: String?,
        breed: String,
        sex: String?,
        birthDate: String
    ): Resource<Animal> =
        try {
            val request = UpdateAnimalRequest(
                name = name,
                breed = breed,
                sex = sex,
                birthDate = birthDate,
                initialWeightKg = current.weightKg,
                photoUrl = current.photoUrl?.takeIf { it.startsWith("http") },
                farmId = current.farmId
            )
            val response = service.updateAnimal(current.id, request)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val animal = body.toAnimal(current.photoUrl)
                dao.insert(animal.toEntity())
                Resource.Success(animal)
            } else {
                Resource.Error(response.errorMessage())
            }
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }
}

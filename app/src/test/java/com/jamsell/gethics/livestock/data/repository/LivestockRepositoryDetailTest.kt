package com.jamsell.gethics.livestock.data.repository

import com.google.gson.Gson
import com.jamsell.gethics.livestock.data.AnimalListResponse
import com.jamsell.gethics.livestock.data.AnimalResponse
import com.jamsell.gethics.livestock.data.LivestockService
import com.jamsell.gethics.livestock.data.RegisterAnimalRequest
import com.jamsell.gethics.livestock.data.UpdateAnimalRequest
import com.jamsell.gethics.livestock.data.local.AnimalDao
import com.jamsell.gethics.livestock.data.local.AnimalEntity
import com.jamsell.gethics.livestock.domain.model.Animal
import com.jamsell.gethics.shared.common.Resource
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class LivestockRepositoryDetailTest {

    private fun dto(
        name: String? = null,
        breed: String = "Holstein",
        sex: String? = "FEMALE",
        photoUrl: String? = null,
        weight: Double? = 650.0,
        farmId: String? = "farm-1"
    ) = AnimalResponse(
        id = "a1", farmId = farmId, tag = "MX-1", qrCode = "GTH-1", name = name, breed = breed, sex = sex,
        birthDate = "2022-05-12", initialWeightKg = weight, photoUrl = photoUrl, status = "ACTIVE"
    )

    private class FakeService(
        val get: () -> Response<AnimalResponse> = { error("no usado") },
        val update: (UpdateAnimalRequest) -> Response<AnimalResponse> = { error("no usado") }
    ) : LivestockService {
        var requestedId: String? = null
        var updatedId: String? = null
        var sent: UpdateAnimalRequest? = null

        override suspend fun getAnimal(animalId: String): Response<AnimalResponse> {
            requestedId = animalId
            return get()
        }

        override suspend fun updateAnimal(animalId: String, request: UpdateAnimalRequest): Response<AnimalResponse> {
            updatedId = animalId
            sent = request
            return update(request)
        }

        override suspend fun registerAnimal(request: RegisterAnimalRequest): Response<AnimalResponse> = error("no usado")
        override suspend fun getAnimals(search: String?, status: String?): Response<AnimalListResponse> = error("no usado")
    }

    private class FakeDao(vararg initial: AnimalEntity) : AnimalDao {
        val saved = mutableListOf(*initial)
        override suspend fun insert(animal: AnimalEntity) { saved.removeAll { it.id == animal.id }; saved += animal }
        override suspend fun insertAll(animals: List<AnimalEntity>) { animals.forEach { insert(it) } }
        override suspend fun fetchAll() = saved.toList()
        override suspend fun fetchById(id: String) = saved.firstOrNull { it.id == id }
        override suspend fun search(pattern: String, status: String) = emptyList<AnimalEntity>()
        override suspend fun deleteByStatus(status: String) { saved.removeAll { it.status == status } }
    }

    private fun entity(photoUrl: String? = null) =
        AnimalEntity("a1", "Luna", "MX-1", "Holstein", photoUrl, "2022-05-12", 650.0, "GTH-1", "ACTIVE", "FEMALE", "farm-1")

    private fun <T> error(code: Int, body: String): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    private val current = Animal(
        id = "a1", name = "Luna", tag = "MX-1", breed = "Holstein", photoUrl = "content://foto/1",
        birthDate = "2022-05-12", weightKg = 650.0, qrCode = "GTH-1", status = "ACTIVE", sex = "FEMALE", farmId = "farm-1"
    )

    private fun get(service: FakeService, dao: FakeDao = FakeDao()) =
        runBlocking { LivestockRepository(service, dao).getAnimal("a1") }

    private fun update(service: FakeService, dao: FakeDao = FakeDao(), animal: Animal = current) =
        runBlocking { LivestockRepository(service, dao).updateAnimal(animal, "Estrella", "Jersey", "MALE", "2023-01-02") }

    // ---------------- getAnimal ----------------

    @Test
    fun `200 devuelve el animal y lo guarda en Room`() {
        val dao = FakeDao()
        val service = FakeService(get = { Response.success(dto(name = "Luna")) })
        val result = get(service, dao)

        assertTrue(result is Resource.Success)
        assertEquals("a1", service.requestedId)
        assertEquals("Luna", result.data?.name)
        assertEquals("FEMALE", result.data?.sex)
        assertEquals("farm-1", result.data?.farmId)
        assertEquals(listOf("a1"), dao.saved.map { it.id })
    }

    @Test
    fun `la ficha conserva la foto local guardada cuando el backend no trae photoUrl`() {
        val dao = FakeDao(entity(photoUrl = "content://foto/1"))
        val result = get(FakeService(get = { Response.success(dto()) }), dao)

        assertEquals("content://foto/1", result.data?.photoUrl)
        assertEquals("content://foto/1", dao.saved.single().photoUrl)
    }

    @Test
    fun `la ficha sin conexion devuelve error de conexion con el animal guardado`() {
        val result = get(FakeService(get = { throw IOException() }), FakeDao(entity()))

        assertTrue(result is Resource.Error)
        assertEquals(CONNECTION_ERROR, result.message)
        assertEquals("a1", result.data?.id)
    }

    @Test
    fun `la ficha sin conexion y sin guardado devuelve error sin datos`() {
        val result = get(FakeService(get = { throw IOException() }))

        assertEquals(CONNECTION_ERROR, result.message)
        assertNull(result.data)
    }

    @Test
    fun `404 devuelve el message del backend sin datos aunque este guardado`() {
        val result = get(FakeService(get = { error(404, """{"message":"El animal no existe."}""") }), FakeDao(entity()))

        assertEquals("El animal no existe.", result.message)
        assertNull(result.data)
    }

    // ---------------- updateAnimal ----------------

    @Test
    fun `200 devuelve el animal editado y actualiza Room`() {
        val dao = FakeDao(entity())
        val service = FakeService(update = { Response.success(dto(name = "Estrella", breed = "Jersey", sex = "MALE")) })
        val result = update(service, dao)

        assertTrue(result is Resource.Success)
        assertEquals("a1", service.updatedId)
        assertEquals("Estrella", result.data?.name)
        assertEquals("Jersey", dao.saved.single().breed)
        assertEquals("MALE", dao.saved.single().sex)
    }

    @Test
    fun `reenvia peso y granja actuales porque el PUT reemplaza lo que no se envia`() {
        val service = FakeService(update = { Response.success(dto()) })
        update(service)

        assertEquals(
            UpdateAnimalRequest("Estrella", "Jersey", "MALE", "2023-01-02", 650.0, null, "farm-1"),
            service.sent
        )
    }

    @Test
    fun `la foto local nunca se envia pero una URL del backend si`() {
        val local = FakeService(update = { Response.success(dto()) })
        update(local)
        assertNull(local.sent?.photoUrl)

        val remote = FakeService(update = { Response.success(dto(photoUrl = "https://img/1.jpg")) })
        update(remote, animal = current.copy(photoUrl = "https://img/1.jpg"))
        assertEquals("https://img/1.jpg", remote.sent?.photoUrl)
    }

    @Test
    fun `nombre y sexo vacios no se envian en el JSON`() {
        val json = Gson().toJson(UpdateAnimalRequest(null, "Jersey", null, "2023-01-02", null, null, null))

        assertTrue(!json.contains("name") && !json.contains("sex") && !json.contains("farmId") && !json.contains("photoUrl"))
    }

    @Test
    fun `la edicion conserva la foto local en Room`() {
        val dao = FakeDao(entity(photoUrl = "content://foto/1"))
        val result = update(FakeService(update = { Response.success(dto(name = "Estrella")) }), dao)

        assertEquals("content://foto/1", result.data?.photoUrl)
        assertEquals("content://foto/1", dao.saved.single().photoUrl)
    }

    @Test
    fun `400 devuelve el message del backend y no toca Room`() {
        val dao = FakeDao(entity())
        val body = """{"message":"La fecha de nacimiento no puede ser posterior a hoy."}"""
        val result = update(FakeService(update = { error(400, body) }), dao)

        assertEquals("La fecha de nacimiento no puede ser posterior a hoy.", result.message)
        assertEquals("Holstein", dao.saved.single().breed)
    }

    @Test
    fun `editar sin conexion devuelve error de conexion y no toca Room`() {
        val dao = FakeDao(entity())
        val result = update(FakeService(update = { throw IOException() }), dao)

        assertEquals(CONNECTION_ERROR, result.message)
        assertEquals("Holstein", dao.saved.single().breed)
    }
}

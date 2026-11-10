package com.jamsell.gethics.livestock.data.repository

import com.jamsell.gethics.livestock.data.AnimalListResponse
import com.jamsell.gethics.livestock.data.AnimalResponse
import com.jamsell.gethics.livestock.data.LivestockService
import com.jamsell.gethics.livestock.data.RegisterAnimalRequest
import com.jamsell.gethics.livestock.data.UpdateAnimalRequest
import com.jamsell.gethics.livestock.data.local.AnimalDao
import com.jamsell.gethics.livestock.data.local.AnimalEntity
import com.jamsell.gethics.shared.common.Resource
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/** US08: la baja logica llama al DELETE y deja el animal INACTIVE en Room (nunca lo borra). */
class LivestockRepositoryDeactivateTest {

    private class FakeService(val deactivate: () -> Response<Unit>) : LivestockService {
        var deactivatedId: String? = null
        var calls = 0

        override suspend fun deactivateAnimal(animalId: String): Response<Unit> {
            calls++
            deactivatedId = animalId
            return deactivate()
        }

        override suspend fun getAnimal(animalId: String): Response<AnimalResponse> = error("no usado")
        override suspend fun updateAnimal(animalId: String, request: UpdateAnimalRequest): Response<AnimalResponse> = error("no usado")
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

    private fun entity(id: String = "a1", status: String = "ACTIVE") =
        AnimalEntity(id, "Luna", "MX-$id", "Holstein", "content://foto/1", "2022-05-12", 650.0, "GTH-$id", status, "FEMALE", "farm-1")

    private fun failure(code: Int, body: String): Response<Unit> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    private fun deactivate(service: FakeService, dao: FakeDao = FakeDao(entity())) =
        runBlocking { LivestockRepository(service, dao).deactivateAnimal("a1") }

    @Test
    fun `204 devuelve exito y llama al DELETE del animal`() {
        val service = FakeService { Response.success(Unit) }

        val result = deactivate(service)

        assertTrue(result is Resource.Success)
        assertEquals("a1", service.deactivatedId)
    }

    @Test
    fun `tras la baja el animal queda INACTIVE en Room y conserva sus datos y su foto`() {
        val dao = FakeDao(entity("a1"), entity("a2"))

        deactivate(FakeService { Response.success(Unit) }, dao)

        val baja = dao.saved.single { it.id == "a1" }
        assertEquals("INACTIVE", baja.status)
        assertEquals("Luna", baja.name)
        assertEquals("content://foto/1", baja.photoUrl)
        assertEquals(2, dao.saved.size)
        assertEquals("ACTIVE", dao.saved.single { it.id == "a2" }.status)
    }

    @Test
    fun `la baja de un animal que no esta en Room no falla ni inventa datos`() {
        val dao = FakeDao()

        val result = deactivate(FakeService { Response.success(Unit) }, dao)

        assertTrue(result is Resource.Success)
        assertTrue(dao.saved.isEmpty())
    }

    @Test
    fun `404 devuelve el message del backend y no toca Room`() {
        val dao = FakeDao(entity())

        val result = deactivate(FakeService { failure(404, """{"message":"El animal no existe."}""") }, dao)

        assertTrue(result is Resource.Error)
        assertEquals("El animal no existe.", result.message)
        assertEquals("ACTIVE", dao.saved.single().status)
    }

    @Test
    fun `409 animal ya dado de baja devuelve el message del backend y no toca Room`() {
        val dao = FakeDao(entity())

        val result = deactivate(FakeService { failure(409, """{"message":"El animal ya esta dado de baja."}""") }, dao)

        assertEquals("El animal ya esta dado de baja.", result.message)
        assertEquals("ACTIVE", dao.saved.single().status)
    }

    @Test
    fun `sin conexion devuelve el error de conexion y no toca Room`() {
        val dao = FakeDao(entity())

        val result = deactivate(FakeService { throw IOException() }, dao)

        assertTrue(result is Resource.Error)
        assertEquals(CONNECTION_ERROR, result.message)
        assertEquals("ACTIVE", dao.saved.single().status)
    }
}

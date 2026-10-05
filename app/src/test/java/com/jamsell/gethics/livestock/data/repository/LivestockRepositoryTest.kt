package com.jamsell.gethics.livestock.data.repository

import com.google.gson.Gson
import com.jamsell.gethics.livestock.data.AnimalResponse
import com.jamsell.gethics.livestock.data.LivestockService
import com.jamsell.gethics.livestock.data.RegisterAnimalRequest
import com.jamsell.gethics.livestock.data.local.AnimalDao
import com.jamsell.gethics.livestock.data.local.AnimalEntity
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

class LivestockRepositoryTest {

    private val created = AnimalResponse(
        id = "9b7d4e21-0000-4000-8000-000000000002",
        farmId = null,
        tag = "MX-00123",
        qrCode = "GTH-3F9A1C7B2E40",
        name = null,
        breed = "Holstein",
        sex = null,
        birthDate = "2024-03-01",
        initialWeightKg = 420.5,
        photoUrl = null,
        status = "ACTIVE"
    )

    private class FakeService(val register: (RegisterAnimalRequest) -> Response<AnimalResponse>) : LivestockService {
        var sent: RegisterAnimalRequest? = null
        override suspend fun registerAnimal(request: RegisterAnimalRequest): Response<AnimalResponse> {
            sent = request
            return register(request)
        }
    }

    private class FakeDao : AnimalDao {
        val saved = mutableListOf<AnimalEntity>()
        override suspend fun insert(animal: AnimalEntity) { saved += animal }
        override suspend fun insertAll(animals: List<AnimalEntity>) { saved += animals }
        override suspend fun fetchAll() = saved.toList()
        override suspend fun fetchById(id: String) = saved.firstOrNull { it.id == id }
    }

    private fun <T> error(code: Int, body: String): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    private fun register(service: FakeService, dao: FakeDao = FakeDao()) =
        runBlocking { LivestockRepository(service, dao).registerAnimal("mx-00123", "Holstein", "2024-03-01", 420.5, "content://foto/1") }

    @Test
    fun `201 devuelve el animal del backend y lo guarda en Room`() {
        val dao = FakeDao()
        val result = register(FakeService { Response.success(201, created) }, dao)

        assertTrue(result is Resource.Success)
        assertEquals("MX-00123", result.data?.tag)
        assertEquals("GTH-3F9A1C7B2E40", result.data?.qrCode)
        assertEquals("ACTIVE", result.data?.status)
        assertEquals(1, dao.saved.size)
        assertEquals(created.id, dao.saved.single().id)
    }

    @Test
    fun `sin nombre en la respuesta usa el arete y conserva la foto local`() {
        val result = register(FakeService { Response.success(201, created) })

        assertEquals("MX-00123", result.data?.name)
        assertEquals("content://foto/1", result.data?.photoUrl)
    }

    @Test
    fun `envia arete raza fecha y peso pero no la foto local`() {
        val service = FakeService { Response.success(201, created) }
        register(service)

        assertEquals(RegisterAnimalRequest("mx-00123", "Holstein", "2024-03-01", 420.5), service.sent)
        val json = Gson().toJson(service.sent)
        assertTrue(!json.contains("photo") && !json.contains("content://"))
    }

    @Test
    fun `peso vacio no se envia en el JSON`() {
        val json = Gson().toJson(RegisterAnimalRequest("A-1", "Jersey", "2024-03-01", null))
        assertTrue(!json.contains("initialWeightKg"))
    }

    @Test
    fun `409 arete repetido devuelve el message del backend y no guarda`() {
        val dao = FakeDao()
        val body = """{"message":"Ya existe un animal con el arete MX-00123."}"""
        val result = register(FakeService { error(409, body) }, dao)

        assertTrue(result is Resource.Error)
        assertEquals("Ya existe un animal con el arete MX-00123.", result.message)
        assertTrue(dao.saved.isEmpty())
    }

    @Test
    fun `400 devuelve el message del backend`() {
        val body = """{"message":"La fecha de nacimiento no puede ser posterior a hoy."}"""
        val result = register(FakeService { error(400, body) })

        assertEquals("La fecha de nacimiento no puede ser posterior a hoy.", result.message)
    }

    @Test
    fun `body no parseable devuelve mensaje generico con el codigo`() {
        val result = register(FakeService { error(500, "<html>oops</html>") })

        assertEquals("No se pudo completar la solicitud (HTTP 500)", result.message)
    }

    @Test
    fun `sin conexion devuelve error de conexion y no guarda`() {
        val dao = FakeDao()
        val result = register(FakeService { throw IOException() }, dao)

        assertEquals(CONNECTION_ERROR, result.message)
        assertTrue(dao.saved.isEmpty())
    }

    @Test
    fun `JSON real del backend se deserializa en AnimalResponse`() {
        // Forma de AnimalResource (Jackson incluye los null)
        val json = """
            {"id":"9b7d4e21-0000-4000-8000-000000000002","farmId":null,"tag":"MX-00123","qrCode":"GTH-3F9A1C7B2E40",
             "name":null,"breed":"Holstein","sex":null,"birthDate":"2024-03-01","initialWeightKg":420.50,
             "photoUrl":null,"status":"ACTIVE"}
        """.trimIndent()

        val animal = Gson().fromJson(json, AnimalResponse::class.java)

        assertEquals("GTH-3F9A1C7B2E40", animal.qrCode)
        assertEquals(420.5, animal.initialWeightKg!!, 0.0)
        assertEquals("2024-03-01", animal.birthDate)
        assertEquals("ACTIVE", animal.status)
        assertNull(animal.farmId)
        assertNull(animal.name)
    }
}

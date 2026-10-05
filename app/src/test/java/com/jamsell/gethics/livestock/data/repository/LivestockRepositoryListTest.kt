package com.jamsell.gethics.livestock.data.repository

import com.google.gson.Gson
import com.jamsell.gethics.livestock.data.AnimalListResponse
import com.jamsell.gethics.livestock.data.AnimalResponse
import com.jamsell.gethics.livestock.data.LivestockService
import com.jamsell.gethics.livestock.data.RegisterAnimalRequest
import com.jamsell.gethics.livestock.data.local.AnimalDao
import com.jamsell.gethics.livestock.data.local.AnimalEntity
import com.jamsell.gethics.livestock.domain.model.AnimalStatus
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

class LivestockRepositoryListTest {

    private fun dto(id: String, tag: String, status: String = "ACTIVE", photoUrl: String? = null) = AnimalResponse(
        id = id, farmId = null, tag = tag, qrCode = "GTH-$tag", name = null, breed = "Holstein", sex = null,
        birthDate = "2024-03-01", initialWeightKg = 420.5, photoUrl = photoUrl, status = status
    )

    private fun entity(id: String, tag: String, status: String = "ACTIVE", photoUrl: String? = null) =
        AnimalEntity(id, tag, tag, "Holstein", photoUrl, "2024-03-01", 420.5, "GTH-$tag", status)

    private class FakeService(val list: (String?, String?) -> Response<AnimalListResponse>) : LivestockService {
        var searchSent: String? = "no llamado"
        var statusSent: String? = "no llamado"
        override suspend fun getAnimals(search: String?, status: String?): Response<AnimalListResponse> {
            searchSent = search
            statusSent = status
            return list(search, status)
        }

        override suspend fun registerAnimal(request: RegisterAnimalRequest): Response<AnimalResponse> = error("no usado")
    }

    private class FakeDao(vararg initial: AnimalEntity) : AnimalDao {
        val saved = mutableListOf(*initial)
        var patternSent: String? = null
        override suspend fun insert(animal: AnimalEntity) { saved.removeAll { it.id == animal.id }; saved += animal }
        override suspend fun insertAll(animals: List<AnimalEntity>) { animals.forEach { insert(it) } }
        override suspend fun fetchAll() = saved.toList()
        override suspend fun fetchById(id: String) = saved.firstOrNull { it.id == id }
        override suspend fun search(pattern: String, status: String): List<AnimalEntity> {
            patternSent = pattern
            return saved.filter { it.status == status }.sortedBy { it.tag }
        }
        override suspend fun deleteByStatus(status: String) { saved.removeAll { it.status == status } }
    }

    private fun ok(vararg animals: AnimalResponse, message: String? = null) =
        Response.success(AnimalListResponse(animals.toList(), message))

    private fun <T> error(code: Int, body: String): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    private fun load(service: FakeService, dao: FakeDao = FakeDao(), search: String? = null, status: AnimalStatus = AnimalStatus.ACTIVE) =
        runBlocking { LivestockRepository(service, dao).getAnimals(search, status) }

    @Test
    fun `200 devuelve los animales y consulta ACTIVE sin busqueda`() {
        val service = FakeService { _, _ -> ok(dto("1", "MX-1"), dto("2", "MX-2")) }
        val result = load(service)

        assertTrue(result is Resource.Success)
        assertEquals(listOf("MX-1", "MX-2"), result.data?.map { it.tag })
        assertNull(service.searchSent)
        assertEquals("ACTIVE", service.statusSent)
    }

    @Test
    fun `la busqueda se envia sin espacios y la vacia no se envia`() {
        val service = FakeService { _, _ -> ok() }
        load(service, search = "  holstein ", status = AnimalStatus.SOLD)
        assertEquals("holstein", service.searchSent)
        assertEquals("SOLD", service.statusSent)

        load(service, search = "   ")
        assertNull(service.searchSent)
    }

    @Test
    fun `sin busqueda reemplaza el cache del estado y deja los demas`() {
        val dao = FakeDao(entity("old", "VIEJO", "ACTIVE"), entity("sold", "VENDIDO", "SOLD"))
        load(FakeService { _, _ -> ok(dto("1", "MX-1")) }, dao)

        assertEquals(setOf("1", "sold"), dao.saved.map { it.id }.toSet())
    }

    @Test
    fun `con busqueda solo agrega al cache y no borra los demas`() {
        val dao = FakeDao(entity("old", "VIEJO", "ACTIVE"))
        load(FakeService { _, _ -> ok(dto("1", "MX-1")) }, dao, search = "mx")

        assertEquals(setOf("1", "old"), dao.saved.map { it.id }.toSet())
    }

    @Test
    fun `conserva la foto local cuando el backend no trae photoUrl`() {
        val dao = FakeDao(entity("1", "MX-1", photoUrl = "content://foto/1"))
        val result = load(FakeService { _, _ -> ok(dto("1", "MX-1"), dto("2", "MX-2")) }, dao)

        assertEquals("content://foto/1", result.data?.first { it.id == "1" }?.photoUrl)
        assertNull(result.data?.first { it.id == "2" }?.photoUrl)
        assertEquals("content://foto/1", dao.saved.first { it.id == "1" }.photoUrl)
    }

    @Test
    fun `sin conexion devuelve error de conexion con los animales guardados del estado`() {
        val dao = FakeDao(entity("1", "MX-2"), entity("2", "MX-1"), entity("3", "VENDIDO", "SOLD"))
        val result = load(FakeService { _, _ -> throw IOException() }, dao, search = "MX_1")

        assertTrue(result is Resource.Error)
        assertEquals(CONNECTION_ERROR, result.message)
        assertEquals(listOf("MX-1", "MX-2"), result.data?.map { it.tag })
        assertEquals("%mx!_1%", dao.patternSent)
    }

    @Test
    fun `400 devuelve el message del backend y sin datos`() {
        val body = """{"message":"Parametro invalido: status"}"""
        val result = load(FakeService { _, _ -> error(400, body) })

        assertTrue(result is Resource.Error)
        assertEquals("Parametro invalido: status", result.message)
        assertNull(result.data)
    }

    @Test
    fun `patron de busqueda`() {
        assertEquals("%", searchPattern(null))
        assertEquals("%", searchPattern("  "))
        assertEquals("%holstein%", searchPattern("  HolStein "))
        assertEquals("%100!%%", searchPattern("100%"))
        assertEquals("%a!!b%", searchPattern("a!b"))
    }

    @Test
    fun `JSON real de la lista se deserializa`() {
        val json = """
            {"animals":[{"id":"9b7d4e21-0000-4000-8000-000000000002","farmId":null,"tag":"MX-00123","qrCode":"GTH-3F9A1C7B2E40",
              "name":null,"breed":"Holstein","sex":null,"birthDate":"2024-03-01","initialWeightKg":420.50,
              "photoUrl":null,"status":"ACTIVE"}],"message":null}
        """.trimIndent()
        val empty = """{"animals":[],"message":"Sin resultados."}"""

        val list = Gson().fromJson(json, AnimalListResponse::class.java)
        val none = Gson().fromJson(empty, AnimalListResponse::class.java)

        assertEquals("MX-00123", list.animals.single().tag)
        assertNull(list.message)
        assertTrue(none.animals.isEmpty())
        assertEquals("Sin resultados.", none.message)
    }
}

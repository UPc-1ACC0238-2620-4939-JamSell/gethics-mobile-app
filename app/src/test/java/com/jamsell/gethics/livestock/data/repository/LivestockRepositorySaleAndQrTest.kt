package com.jamsell.gethics.livestock.data.repository

import com.jamsell.gethics.livestock.data.AnimalListResponse
import com.jamsell.gethics.livestock.data.AnimalResponse
import com.jamsell.gethics.livestock.data.AnimalSaleResponse
import com.jamsell.gethics.livestock.data.LivestockService
import com.jamsell.gethics.livestock.data.RegisterAnimalRequest
import com.jamsell.gethics.livestock.data.SellAnimalRequest
import com.jamsell.gethics.livestock.data.UpdateAnimalRequest
import com.jamsell.gethics.livestock.data.local.AnimalDao
import com.jamsell.gethics.livestock.data.local.AnimalEntity
import com.jamsell.gethics.livestock.domain.model.Animal
import com.jamsell.gethics.shared.common.Resource
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/** US-30 (venta) y US-29 (buscar por QR) en el repositorio. */
class LivestockRepositorySaleAndQrTest {

    private class FakeService(
        val sale: () -> Response<AnimalSaleResponse> = { error("no usado") },
        val qr: () -> Response<AnimalResponse> = { error("no usado") }
    ) : LivestockService {
        var lastSale: SellAnimalRequest? = null
        var lastQr: String? = null

        override suspend fun sellAnimal(animalId: String, request: SellAnimalRequest): Response<AnimalSaleResponse> {
            lastSale = request
            return sale()
        }

        override suspend fun getAnimalByQr(code: String): Response<AnimalResponse> {
            lastQr = code
            return qr()
        }

        override suspend fun deactivateAnimal(animalId: String): Response<Unit> = error("no usado")
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

    private fun entity(id: String = "a1", farmId: String? = "farm-1") =
        AnimalEntity(id, "Luna", "MX-$id", "Holstein", "content://foto/1", "2022-05-12", 650.0, "GTH-$id", "ACTIVE", "FEMALE", farmId)

    private fun animal(farmId: String? = "farm-1") =
        Animal("a1", "Luna", "MX-a1", "Holstein", null, "2022-05-12", 650.0, "GTH-a1", "ACTIVE", "FEMALE", farmId)

    private fun saleBody() = AnimalSaleResponse("a1", "SOLD", 1500.5, "2026-11-10", "tx-1")

    private fun qrBody() = AnimalResponse("a1", "farm-1", "MX-A1", "GTH-A1", "Luna", "Holstein", "FEMALE", "2022-05-12", 650.0, null, "ACTIVE")

    private fun <T> failure(code: Int, body: String): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    // ---------------- US-30 ----------------

    @Test
    fun `venta exitosa deja el animal SOLD en Room y conserva sus datos`() {
        val dao = FakeDao(entity("a1"), entity("a2"))
        val service = FakeService(sale = { Response.success(saleBody()) })

        val result = runBlocking { LivestockRepository(service, dao).sellAnimal(animal(), 1500.5, "2026-11-10", "owner-1") }

        assertTrue(result is Resource.Success)
        assertEquals(1500.5, service.lastSale?.price ?: 0.0, 0.0)
        assertEquals("2026-11-10", service.lastSale?.saleDate)
        val sold = dao.saved.single { it.id == "a1" }
        assertEquals("SOLD", sold.status)
        assertEquals("Luna", sold.name)
        assertEquals("content://foto/1", sold.photoUrl)
        assertEquals("ACTIVE", dao.saved.single { it.id == "a2" }.status)
    }

    @Test
    fun `con granja no se envia el ownerId`() {
        val service = FakeService(sale = { Response.success(saleBody()) })

        runBlocking { LivestockRepository(service, FakeDao(entity())).sellAnimal(animal(farmId = "farm-1"), 10.0, "2026-11-10", "owner-1") }

        assertNull(service.lastSale?.ownerId)
    }

    @Test
    fun `sin granja se envia el ownerId de la sesion`() {
        val service = FakeService(sale = { Response.success(saleBody()) })

        runBlocking { LivestockRepository(service, FakeDao(entity(farmId = null))).sellAnimal(animal(farmId = null), 10.0, "2026-11-10", "owner-1") }

        assertEquals("owner-1", service.lastSale?.ownerId)
    }

    @Test
    fun `409 animal no activo devuelve el message del backend y no toca Room`() {
        val dao = FakeDao(entity())
        val service = FakeService(sale = { failure(409, """{"message":"El animal ya no esta activo."}""") })

        val result = runBlocking { LivestockRepository(service, dao).sellAnimal(animal(), 10.0, "2026-11-10", "owner-1") }

        assertTrue(result is Resource.Error)
        assertEquals("El animal ya no esta activo.", result.message)
        assertEquals("ACTIVE", dao.saved.single().status)
    }

    @Test
    fun `sin conexion la venta devuelve el error de conexion y no toca Room`() {
        val dao = FakeDao(entity())
        val service = FakeService(sale = { throw IOException() })

        val result = runBlocking { LivestockRepository(service, dao).sellAnimal(animal(), 10.0, "2026-11-10", "owner-1") }

        assertEquals(CONNECTION_ERROR, result.message)
        assertEquals("ACTIVE", dao.saved.single().status)
    }

    // ---------------- US-29 ----------------

    @Test
    fun `QR encontrado devuelve el animal, lo guarda en Room y conserva la foto local`() {
        val dao = FakeDao(entity("a1"))
        val service = FakeService(qr = { Response.success(qrBody()) })

        val result = runBlocking { LivestockRepository(service, dao).getAnimalByQr("  GTH-A1 ") }

        assertTrue(result is Resource.Success)
        assertEquals("GTH-A1", service.lastQr)
        assertEquals("a1", result.data?.id)
        assertEquals("content://foto/1", dao.saved.single { it.id == "a1" }.photoUrl)
    }

    @Test
    fun `QR inexistente 404 devuelve el message del backend sin animal`() {
        val service = FakeService(qr = { failure(404, """{"message":"No hay ningun animal con ese codigo QR."}""") })

        val result = runBlocking { LivestockRepository(service, FakeDao()).getAnimalByQr("GTH-X") }

        assertTrue(result is Resource.Error)
        assertEquals("No hay ningun animal con ese codigo QR.", result.message)
        assertNull(result.data)
    }

    @Test
    fun `sin conexion un QR de un animal guardado lo devuelve igual, sin importar mayusculas`() {
        val service = FakeService(qr = { throw IOException() })

        val result = runBlocking { LivestockRepository(service, FakeDao(entity("a1"))).getAnimalByQr("gth-a1") }

        assertTrue(result is Resource.Error)
        assertEquals(CONNECTION_ERROR, result.message)
        assertNotNull(result.data)
        assertEquals("a1", result.data?.id)
    }

    @Test
    fun `sin conexion un QR que no esta guardado devuelve solo el error de conexion`() {
        val service = FakeService(qr = { throw IOException() })

        val result = runBlocking { LivestockRepository(service, FakeDao(entity("a1"))).getAnimalByQr("GTH-OTRO") }

        assertEquals(CONNECTION_ERROR, result.message)
        assertNull(result.data)
    }
}

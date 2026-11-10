package com.jamsell.gethics.livestock

import com.jamsell.gethics.livestock.data.AnimalListResponse
import com.jamsell.gethics.livestock.data.AnimalResponse
import com.jamsell.gethics.livestock.data.AnimalSaleResponse
import com.jamsell.gethics.livestock.data.LastSyncStore
import com.jamsell.gethics.livestock.data.LivestockService
import com.jamsell.gethics.livestock.data.RegisterAnimalRequest
import com.jamsell.gethics.livestock.data.SellAnimalRequest
import com.jamsell.gethics.livestock.data.UpdateAnimalRequest
import com.jamsell.gethics.livestock.data.local.AnimalDao
import com.jamsell.gethics.livestock.data.local.AnimalEntity
import com.jamsell.gethics.livestock.data.repository.LivestockRepository
import com.jamsell.gethics.livestock.domain.model.AnimalStatus
import com.jamsell.gethics.livestock.presentation.formatLastUpdate
import com.jamsell.gethics.livestock.presentation.qrMatrix
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.time.ZoneOffset

/** US-36 (fecha de ultima actualizacion) y US-29 (QR como imagen). */
class OfflineAndQrImageTest {

    private class MemoryStore : LastSyncStore {
        var value: Long? = null
        override fun save(epochMillis: Long) { value = epochMillis }
        override fun get() = value
    }

    private class FakeService(val list: () -> Response<AnimalListResponse>, val one: () -> Response<AnimalResponse> = { error("no usado") }) : LivestockService {
        override suspend fun getAnimals(search: String?, status: String?) = list()
        override suspend fun getAnimal(animalId: String) = one()
        override suspend fun getAnimalByQr(code: String): Response<AnimalResponse> = error("no usado")
        override suspend fun sellAnimal(animalId: String, request: SellAnimalRequest): Response<AnimalSaleResponse> = error("no usado")
        override suspend fun deactivateAnimal(animalId: String): Response<Unit> = error("no usado")
        override suspend fun updateAnimal(animalId: String, request: UpdateAnimalRequest): Response<AnimalResponse> = error("no usado")
        override suspend fun registerAnimal(request: RegisterAnimalRequest): Response<AnimalResponse> = error("no usado")
    }

    private class FakeDao : AnimalDao {
        val saved = mutableListOf<AnimalEntity>()
        override suspend fun insert(animal: AnimalEntity) { saved.removeAll { it.id == animal.id }; saved += animal }
        override suspend fun insertAll(animals: List<AnimalEntity>) { animals.forEach { insert(it) } }
        override suspend fun fetchAll() = saved.toList()
        override suspend fun fetchById(id: String) = saved.firstOrNull { it.id == id }
        override suspend fun search(pattern: String, status: String) = saved.filter { it.status == status }
        override suspend fun deleteByStatus(status: String) { saved.removeAll { it.status == status } }
    }

    private fun body() = AnimalResponse("a1", null, "MX-1", "GTH-1", "Luna", "Holstein", "FEMALE", "2022-05-12", 650.0, null, "ACTIVE")

    // ---------------- US-36 ----------------

    @Test
    fun `formatLastUpdate usa dd-MM-yyyy HH-mm en la zona del dispositivo y null si nunca se actualizo`() {
        assertEquals("10/11/2026 14:30", formatLastUpdate(1794339000000L, ZoneOffset.ofHours(-5)))
        assertNull(formatLastUpdate(null))
    }

    @Test
    fun `una lista traida del backend guarda la fecha de actualizacion`() {
        val store = MemoryStore()
        val repository = LivestockRepository(FakeService({ Response.success(AnimalListResponse(listOf(body()), null)) }), FakeDao(), store) { 123L }

        runBlocking { repository.getAnimals(null, AnimalStatus.ACTIVE) }

        assertEquals(123L, repository.lastSyncMillis())
    }

    @Test
    fun `una ficha traida del backend tambien guarda la fecha`() {
        val store = MemoryStore()
        val repository = LivestockRepository(FakeService({ error("no usado") }, { Response.success(body()) }), FakeDao(), store) { 456L }

        runBlocking { repository.getAnimal("a1") }

        assertEquals(456L, store.value)
    }

    @Test
    fun `sin conexion la fecha guardada no cambia y los animales guardados siguen disponibles`() {
        val store = MemoryStore().also { it.value = 111L }
        val dao = FakeDao().also { it.saved += AnimalEntity("a1", "Luna", "MX-1", "Holstein", null, null, null, "GTH-1", "ACTIVE", null, null) }
        val repository = LivestockRepository(FakeService({ throw IOException() }), dao, store) { 999L }

        val result = runBlocking { repository.getAnimals(null, AnimalStatus.ACTIVE) }

        assertEquals(111L, repository.lastSyncMillis())
        assertEquals(1, result.data?.size)
    }

    @Test
    fun `sin almacenamiento de fecha el repositorio devuelve null`() {
        assertNull(LivestockRepository(FakeService({ throw IOException() }), FakeDao()).lastSyncMillis())
    }

    // ---------------- US-29 ----------------

    @Test
    fun `qrMatrix genera una matriz cuadrada con modulos oscuros y claros`() {
        val matrix = qrMatrix("GTH-ABC123456789")
        assertNotNull(matrix)
        assertTrue(matrix!!.size >= 21)
        assertTrue(matrix.all { it.size == matrix.size })
        assertTrue(matrix.any { row -> row.any { it } })
        assertTrue(matrix.any { row -> row.any { !it } })
    }

    @Test
    fun `qrMatrix da la misma matriz para el mismo codigo y otra distinta para otro codigo`() {
        val a = qrMatrix("GTH-AAAAAAAAAAAA")!!
        assertTrue(a.contentDeepEquals(qrMatrix(" GTH-AAAAAAAAAAAA ")!!))
        assertTrue(!a.contentDeepEquals(qrMatrix("GTH-BBBBBBBBBBBB")!!))
    }

    @Test
    fun `qrMatrix sin codigo devuelve null`() {
        assertNull(qrMatrix(null))
        assertNull(qrMatrix("   "))
    }
}


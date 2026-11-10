package com.jamsell.gethics.livestock.data.repository

import com.jamsell.gethics.livestock.data.FarmListResponse
import com.jamsell.gethics.livestock.data.FarmResponse
import com.jamsell.gethics.livestock.data.FarmService
import com.jamsell.gethics.livestock.data.UpdateFarmRequest
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

/** US-32: cargar y editar una granja. */
class FarmRepositoryTest {

    private class FakeFarmService(
        val list: () -> Response<FarmListResponse> = { error("no usado") },
        val update: () -> Response<FarmResponse> = { error("no usado") }
    ) : FarmService {
        var lastOwner: String? = null
        var lastUpdate: Pair<String, UpdateFarmRequest>? = null

        override suspend fun getFarms(ownerId: String): Response<FarmListResponse> {
            lastOwner = ownerId
            return list()
        }

        override suspend fun updateFarm(farmId: String, request: UpdateFarmRequest): Response<FarmResponse> {
            lastUpdate = farmId to request
            return update()
        }
    }

    private fun farm(id: String, name: String = "La Esperanza", size: Double? = 12.5) = FarmResponse(id, "owner-1", name, "Cusco", size, "ACTIVE")

    private fun <T> failure(code: Int, body: String): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    @Test
    fun `getFarm busca la granja entre las del dueno`() {
        val service = FakeFarmService(list = { Response.success(FarmListResponse(listOf(farm("f1"), farm("f2", "Otra")), null)) })

        val result = runBlocking { FarmRepository(service).getFarm("owner-1", "f2") }

        assertTrue(result is Resource.Success)
        assertEquals("Otra", result.data?.name)
        assertEquals("owner-1", service.lastOwner)
    }

    @Test
    fun `getFarm con una granja que ya no esta devuelve error`() {
        val service = FakeFarmService(list = { Response.success(FarmListResponse(emptyList(), "No hay granjas registradas.")) })

        val result = runBlocking { FarmRepository(service).getFarm("owner-1", "f1") }

        assertTrue(result is Resource.Error)
        assertEquals(FARM_NOT_FOUND, result.message)
    }

    @Test
    fun `getFarm sin conexion devuelve el error de conexion`() {
        val result = runBlocking { FarmRepository(FakeFarmService(list = { throw IOException() })).getFarm("owner-1", "f1") }

        assertEquals(CONNECTION_ERROR, result.message)
    }

    @Test
    fun `updateFarm envia los tres campos y devuelve la granja actualizada`() {
        val service = FakeFarmService(update = { Response.success(farm("f1", "Nueva", 20.0)) })

        val result = runBlocking { FarmRepository(service).updateFarm("f1", "Nueva", "Cusco", 20.0) }

        assertTrue(result is Resource.Success)
        assertEquals("Nueva", result.data?.name)
        assertEquals("f1", service.lastUpdate?.first)
        assertEquals(UpdateFarmRequest("Nueva", "Cusco", 20.0), service.lastUpdate?.second)
    }

    @Test
    fun `updateFarm sin tamano envia null`() {
        val service = FakeFarmService(update = { Response.success(farm("f1", size = null)) })

        runBlocking { FarmRepository(service).updateFarm("f1", "Nueva", "Cusco", null) }

        assertNull(service.lastUpdate?.second?.sizeHectares)
    }

    @Test
    fun `updateFarm 409 nombre repetido devuelve el message del backend`() {
        val service = FakeFarmService(update = { failure(409, """{"message":"Ya tienes otra granja con ese nombre."}""") })

        val result = runBlocking { FarmRepository(service).updateFarm("f1", "Dup", "Cusco", null) }

        assertTrue(result is Resource.Error)
        assertEquals("Ya tienes otra granja con ese nombre.", result.message)
    }

    @Test
    fun `updateFarm sin conexion devuelve el error de conexion`() {
        val result = runBlocking { FarmRepository(FakeFarmService(update = { throw IOException() })).updateFarm("f1", "N", "U", null) }

        assertEquals(CONNECTION_ERROR, result.message)
    }
}

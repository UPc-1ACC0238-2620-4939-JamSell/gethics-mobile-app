package com.jamsell.gethics.sanitary.data.repository

import com.jamsell.gethics.sanitary.data.RegisterSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.sanitary.data.SanitaryService
import com.jamsell.gethics.shared.common.Resource
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class SanitaryRepositoryTest {

    private val request = RegisterSanitaryEventRequest(SanitaryEventType.TREATMENT, "2026-10-04T00:00:00", null)

    private fun repositoryReturning(block: () -> Response<Unit>) = SanitaryRepository(object : SanitaryService {
        override suspend fun registerEvent(animalId: String, request: RegisterSanitaryEventRequest) = block()
    })

    private fun error(code: Int, body: String): Response<Unit> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    @Test
    fun `201 devuelve Success`() = runBlocking {
        val result = repositoryReturning { Response.success(201, Unit) }.registerEvent("uuid", request)
        assertTrue(result is Resource.Success)
    }

    @Test
    fun `400 devuelve el message del backend`() = runBlocking {
        val body = """{"message":"La fecha del evento no puede ser posterior a hoy."}"""
        val result = repositoryReturning { error(400, body) }.registerEvent("uuid", request)
        assertEquals("La fecha del evento no puede ser posterior a hoy.", result.message)
    }

    @Test
    fun `body no parseable devuelve mensaje generico con el codigo`() = runBlocking {
        val result = repositoryReturning { error(500, "<html>oops</html>") }.registerEvent("uuid", request)
        assertEquals("No se pudo completar la solicitud (HTTP 500)", result.message)
    }

    @Test
    fun `sin conexion devuelve error`() = runBlocking {
        val result = repositoryReturning { throw IOException() }.registerEvent("uuid", request)
        assertEquals("No se pudo conectar con el servidor", result.message)
    }
}

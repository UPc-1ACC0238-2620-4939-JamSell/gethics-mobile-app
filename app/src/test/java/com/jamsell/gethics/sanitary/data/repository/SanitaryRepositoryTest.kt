package com.jamsell.gethics.sanitary.data.repository

import com.google.gson.Gson
import com.jamsell.gethics.sanitary.data.RegisterSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryCalendarResponse
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.sanitary.data.SanitaryService
import com.jamsell.gethics.sanitary.data.ScheduledEventDto
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
import java.time.YearMonth

class SanitaryRepositoryTest {

    private val request = RegisterSanitaryEventRequest(SanitaryEventType.TREATMENT, "2026-10-04T00:00:00", null)

    private class FakeService(
        val register: () -> Response<Unit> = { error("no usado") },
        val calendar: (Int, Int) -> Response<SanitaryCalendarResponse> = { _, _ -> error("no usado") }
    ) : SanitaryService {
        override suspend fun registerEvent(animalId: String, request: RegisterSanitaryEventRequest) = register()
        override suspend fun getCalendar(year: Int, month: Int) = calendar(year, month)
    }

    private fun repositoryReturning(block: () -> Response<Unit>) = SanitaryRepository(FakeService(register = block))

    private fun calendarRepository(block: (Int, Int) -> Response<SanitaryCalendarResponse>) =
        SanitaryRepository(FakeService(calendar = block))

    private fun <T> error(code: Int, body: String): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    // ---------------- US11 ----------------

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
        assertEquals(CONNECTION_ERROR, result.message)
    }

    // ---------------- US12 ----------------

    private val event = ScheduledEventDto(
        "e1", "a1", SanitaryEventType.VACCINATION, "2026-10-05", "Aftosa", "SCHEDULED"
    )

    @Test
    fun `calendario 200 con eventos devuelve los eventos`() = runBlocking {
        val body = SanitaryCalendarResponse(2026, 10, listOf(event), null)
        val result = calendarRepository { _, _ -> Response.success(body) }.getCalendar(YearMonth.of(2026, 10))
        assertEquals(listOf(event), result.data?.events)
    }

    @Test
    fun `calendario 200 vacio devuelve lista vacia y message`() = runBlocking {
        val body = SanitaryCalendarResponse(2026, 11, emptyList(), "No hay actividades pendientes.")
        val result = calendarRepository { _, _ -> Response.success(body) }.getCalendar(YearMonth.of(2026, 11))
        assertTrue(result.data!!.events.isEmpty())
        assertEquals("No hay actividades pendientes.", result.data!!.message)
    }

    @Test
    fun `calendario 400 devuelve el message del backend`() = runBlocking {
        val result = calendarRepository { _, _ -> error(400, """{"message":"Periodo de calendario inválido."}""") }
            .getCalendar(YearMonth.of(2026, 10))
        assertTrue(result is Resource.Error)
        assertEquals("Periodo de calendario inválido.", result.message)
    }

    @Test
    fun `calendario sin conexion devuelve error de conexion`() = runBlocking {
        val result = calendarRepository { _, _ -> throw IOException() }.getCalendar(YearMonth.of(2026, 10))
        assertEquals(CONNECTION_ERROR, result.message)
    }

    @Test
    fun `calendario envia year y monthValue de 1 a 12`() = runBlocking {
        var sent: Pair<Int, Int>? = null
        calendarRepository { year, month ->
            sent = year to month
            Response.success(SanitaryCalendarResponse(year, month, emptyList(), null))
        }.getCalendar(YearMonth.of(2026, 12))
        assertEquals(2026 to 12, sent)
    }

    @Test
    fun `JSON real del backend se deserializa en los DTOs`() {
        // Forma de SanitaryCalendarResource / ScheduledEventResource (Jackson incluye los null)
        val json = """
            {"year":2026,"month":10,"events":[
              {"id":"3f1c2a9e-0000-4000-8000-000000000001","animalId":"9b7d4e21-0000-4000-8000-000000000002",
               "type":"VACCINATION","scheduledDate":"2026-10-05","description":"Aftosa","status":"SCHEDULED"},
              {"id":"3f1c2a9e-0000-4000-8000-000000000003","animalId":"9b7d4e21-0000-4000-8000-000000000002",
               "type":"CHECKUP","scheduledDate":"2026-10-20","description":null,"status":"SCHEDULED"}],
             "message":null}
        """.trimIndent()

        val calendar = Gson().fromJson(json, SanitaryCalendarResponse::class.java)

        assertEquals(2, calendar.events.size)
        assertEquals(SanitaryEventType.VACCINATION, calendar.events[0].type)
        assertEquals("2026-10-05", calendar.events[0].scheduledDate)
        assertNull(calendar.events[1].description)
        assertNull(calendar.message)
    }
}

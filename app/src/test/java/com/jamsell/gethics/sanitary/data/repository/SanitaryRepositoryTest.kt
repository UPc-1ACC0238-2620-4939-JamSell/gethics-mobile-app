package com.jamsell.gethics.sanitary.data.repository

import com.google.gson.Gson
import com.jamsell.gethics.sanitary.data.ClinicalHistoryEventDto
import com.jamsell.gethics.sanitary.data.ClinicalHistoryResponse
import com.jamsell.gethics.sanitary.data.RegisterSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryEventStatus
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
        val calendar: (Int, Int) -> Response<SanitaryCalendarResponse> = { _, _ -> error("no usado") },
        val history: (String) -> Response<ClinicalHistoryResponse> = { error("no usado") }
    ) : SanitaryService {
        override suspend fun registerEvent(animalId: String, request: RegisterSanitaryEventRequest) = register()
        override suspend fun getCalendar(year: Int, month: Int) = calendar(year, month)
        override suspend fun getClinicalHistory(animalId: String) = history(animalId)
    }

    private fun historyRepository(block: (String) -> Response<ClinicalHistoryResponse>) =
        SanitaryRepository(FakeService(history = block))

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

    // ---------------- US14 ----------------

    private val historyEvent = ClinicalHistoryEventDto(
        "e1", SanitaryEventType.VACCINATION, SanitaryEventStatus.COMPLETED, "2026-03-01T10:00:00", null, "Aftosa"
    )

    @Test
    fun `historial 200 con eventos devuelve los eventos`() = runBlocking {
        val body = ClinicalHistoryResponse("a1", listOf(historyEvent), null)
        val result = historyRepository { Response.success(body) }.getClinicalHistory("a1")
        assertEquals(listOf(historyEvent), result.data?.events)
    }

    @Test
    fun `historial 200 vacio devuelve lista vacia y Sin registros`() = runBlocking {
        val body = ClinicalHistoryResponse("a1", emptyList(), "Sin registros.")
        val result = historyRepository { Response.success(body) }.getClinicalHistory("a1")
        assertTrue(result.data!!.events.isEmpty())
        assertEquals("Sin registros.", result.data!!.message)
    }

    @Test
    fun `historial 400 devuelve el message del backend`() = runBlocking {
        val result = historyRepository { error(400, """{"message":"Parametro invalido: animalId"}""") }
            .getClinicalHistory("1")
        assertTrue(result is Resource.Error)
        assertEquals("Parametro invalido: animalId", result.message)
    }

    @Test
    fun `historial sin conexion devuelve error de conexion`() = runBlocking {
        val result = historyRepository { throw IOException() }.getClinicalHistory("a1")
        assertEquals("No se pudo conectar con el servidor", result.message)
    }

    @Test
    fun `historial envia el animalId intacto`() = runBlocking {
        val animalId = "9b7d4e21-0000-4000-8000-000000000002"
        var sent: String? = null
        historyRepository {
            sent = it
            Response.success(ClinicalHistoryResponse(it, emptyList(), "Sin registros."))
        }.getClinicalHistory(animalId)
        assertEquals(animalId, sent)
    }

    @Test
    fun `JSON real del historial se deserializa en los DTOs`() {
        // Forma de ClinicalHistoryResource / ClinicalHistoryEventResource (comparacion estricta en el test del backend)
        val json = """
            {"animalId":"9b7d4e21-0000-4000-8000-000000000002",
             "events":[
               {"id":"11111111-1111-1111-1111-111111111111","type":"VACCINATION","status":"COMPLETED",
                "occurredAt":"2025-12-01T08:30:00","scheduledDate":null,"description":"Aftosa"},
               {"id":"22222222-2222-2222-2222-222222222222","type":"VACCINATION","status":"SCHEDULED",
                "occurredAt":null,"scheduledDate":"2027-01-20","description":null}],
             "message":null}
        """.trimIndent()

        val history = Gson().fromJson(json, ClinicalHistoryResponse::class.java)

        assertEquals("9b7d4e21-0000-4000-8000-000000000002", history.animalId)
        assertEquals(2, history.events.size)
        assertEquals(SanitaryEventStatus.COMPLETED, history.events[0].status)
        assertEquals("2025-12-01T08:30:00", history.events[0].occurredAt)
        assertNull(history.events[0].scheduledDate)
        assertEquals(SanitaryEventStatus.SCHEDULED, history.events[1].status)
        assertNull(history.events[1].occurredAt)
        assertEquals("2027-01-20", history.events[1].scheduledDate)
        assertNull(history.events[1].description)
        assertNull(history.message)
    }
}

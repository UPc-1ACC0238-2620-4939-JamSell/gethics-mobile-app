package com.jamsell.gethics.sanitary.data.sync

import com.jamsell.gethics.sanitary.data.CompleteSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.RegisterSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.sanitary.data.SanitaryService
import com.jamsell.gethics.sanitary.data.ScheduleSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.local.FakePendingSanitaryOperationDao
import com.jamsell.gethics.sanitary.data.local.PendingSanitaryOperationEntity
import com.jamsell.gethics.sanitary.data.local.PendingSanitaryOperationStatus
import com.jamsell.gethics.sanitary.data.local.PendingSanitaryOperationType
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class SanitarySyncerTest {

    private fun register(id: String, createdAt: Long = 1) = PendingSanitaryOperationEntity(
        id, PendingSanitaryOperationType.REGISTER, "animal-uuid", SanitaryEventType.TREATMENT, "2026-11-08T00:00:00", "Ivermectina", createdAt
    )

    private fun schedule(id: String, createdAt: Long = 1) = PendingSanitaryOperationEntity(
        id, PendingSanitaryOperationType.SCHEDULE, "animal-uuid", SanitaryEventType.VACCINATION, "2026-11-20", null, createdAt
    )

    private fun error(code: Int, message: String = "rechazado"): Response<Unit> =
        Response.error(code, """{"message":"$message"}""".toResponseBody("application/json".toMediaType()))

    /** Responde segun el clientRequestId enviado y registra cada envio en orden. */
    private class FakeService(val respond: (String) -> Response<Unit> = { Response.success(201, Unit) }) : SanitaryService {
        val sent = mutableListOf<Any>()

        override suspend fun registerEvent(animalId: String, request: RegisterSanitaryEventRequest): Response<Unit> {
            sent += request
            return respond(request.clientRequestId)
        }

        override suspend fun scheduleEvent(animalId: String, request: ScheduleSanitaryEventRequest): Response<Unit> {
            sent += request
            return respond(request.clientRequestId)
        }

        val sentIds get() = sent.map { if (it is RegisterSanitaryEventRequest) it.clientRequestId else (it as ScheduleSanitaryEventRequest).clientRequestId }

        override suspend fun getCalendar(year: Int, month: Int) = error("no usado")
        override suspend fun getClinicalHistory(animalId: String) = error("no usado")
        override suspend fun completeEvent(animalId: String, eventId: String, request: CompleteSanitaryEventRequest) = error("no usado")
        override suspend fun cancelEvent(animalId: String, eventId: String) = error("no usado")
    }

    private fun sync(dao: FakePendingSanitaryOperationDao, service: FakeService) = runBlocking { SanitarySyncer(service, dao).syncAll() }

    @Test
    fun `201 creado borra la operacion`() {
        val dao = FakePendingSanitaryOperationDao(listOf(register("a")))

        assertEquals(SyncResult.SUCCESS, sync(dao, FakeService { Response.success(201, Unit) }))
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `200 duplicado idempotente tambien borra la operacion`() {
        val dao = FakePendingSanitaryOperationDao(listOf(schedule("a")))

        assertEquals(SyncResult.SUCCESS, sync(dao, FakeService { Response.success(200, Unit) }))
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `IOException devuelve RETRY y deja la operacion intacta`() {
        val operation = register("a")
        val dao = FakePendingSanitaryOperationDao(listOf(operation))

        assertEquals(SyncResult.RETRY, sync(dao, FakeService { throw IOException() }))
        assertEquals(listOf(operation), dao.rows)
    }

    @Test
    fun `500 408 y 429 son recuperables - RETRY sin tocar la fila`() {
        for (code in listOf(500, 503, 408, 429)) {
            val operation = register("a")
            val dao = FakePendingSanitaryOperationDao(listOf(operation))

            assertEquals("HTTP $code", SyncResult.RETRY, sync(dao, FakeService { error(code) }))
            assertEquals("HTTP $code", listOf(operation), dao.rows)
        }
    }

    @Test
    fun `400 404 y 409 son permanentes - FAILED con el message del backend`() {
        for (code in listOf(400, 404, 409)) {
            val dao = FakePendingSanitaryOperationDao(listOf(register("a")))

            assertEquals("HTTP $code", SyncResult.SUCCESS, sync(dao, FakeService { error(code, "motivo $code") }))
            val row = dao.rows.single()
            assertEquals(PendingSanitaryOperationStatus.FAILED, row.status)
            assertEquals("motivo $code", row.lastError)
        }
    }

    @Test
    fun `una operacion rechazada no bloquea la siguiente`() {
        val dao = FakePendingSanitaryOperationDao(listOf(register("mala", createdAt = 1), schedule("buena", createdAt = 2)))
        val service = FakeService { if (it == "mala") error(409) else Response.success(201, Unit) }

        assertEquals(SyncResult.SUCCESS, sync(dao, service))
        assertEquals(listOf("mala", "buena"), service.sentIds)
        assertEquals("mala", dao.rows.single().clientRequestId) // solo queda la FAILED
    }

    @Test
    fun `envia en orden FIFO por createdAt`() {
        val dao = FakePendingSanitaryOperationDao(listOf(schedule("tercera", 30), register("primera", 10), register("segunda", 20)))
        val service = FakeService()

        sync(dao, service)

        assertEquals(listOf("primera", "segunda", "tercera"), service.sentIds)
    }

    @Test
    fun `reconstruye cada request con los datos y el clientRequestId guardados`() {
        val dao = FakePendingSanitaryOperationDao(listOf(register("id-registro", 1), schedule("id-programa", 2)))
        val service = FakeService()

        sync(dao, service)

        assertEquals(
            listOf(
                RegisterSanitaryEventRequest(SanitaryEventType.TREATMENT, "2026-11-08T00:00:00", "Ivermectina", "id-registro"),
                ScheduleSanitaryEventRequest(SanitaryEventType.VACCINATION, "2026-11-20", null, "id-programa")
            ),
            service.sent
        )
    }

    @Test
    fun `el reintento envia el MISMO clientRequestId y no duplica la operacion`() {
        val dao = FakePendingSanitaryOperationDao(listOf(register("id-fijo")))
        var online = false
        val service = FakeService { if (online) Response.success(200, Unit) else throw IOException() }

        assertEquals(SyncResult.RETRY, sync(dao, service))
        online = true
        assertEquals(SyncResult.SUCCESS, sync(dao, service))

        assertEquals(listOf("id-fijo", "id-fijo"), service.sentIds)
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `las operaciones FAILED no se reenvian`() {
        val failed = register("vieja").copy(status = PendingSanitaryOperationStatus.FAILED, lastError = "x")
        val dao = FakePendingSanitaryOperationDao(listOf(failed))
        val service = FakeService()

        assertEquals(SyncResult.SUCCESS, sync(dao, service))
        assertTrue(service.sent.isEmpty())
        assertEquals(listOf(failed), dao.rows)
    }
}

package com.jamsell.gethics.sanitary.presentation.clinical_history

import com.jamsell.gethics.sanitary.data.ClinicalHistoryEventDto
import com.jamsell.gethics.sanitary.data.ClinicalHistoryResponse
import com.jamsell.gethics.sanitary.data.RegisterSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryEventStatus
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.sanitary.data.CompleteSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.ScheduleSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryService
import com.jamsell.gethics.sanitary.data.repository.SanitaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ClinicalHistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val animalId = "9b7d4e21-0000-4000-8000-000000000002"

    private fun event(
        id: String,
        status: SanitaryEventStatus?,
        occurredAt: String? = null,
        scheduledDate: String? = null
    ) = ClinicalHistoryEventDto(id, SanitaryEventType.CHECKUP, status, occurredAt, scheduledDate, null)

    /** Responde con la lista de respuestas en orden (la ultima se repite) y registra los animalId pedidos. */
    private class FakeService(vararg responses: Response<ClinicalHistoryResponse>) : SanitaryService {
        private val queue = responses.toMutableList()
        val requested = mutableListOf<String>()

        override suspend fun getClinicalHistory(animalId: String): Response<ClinicalHistoryResponse> {
            requested += animalId
            return if (queue.size > 1) queue.removeAt(0) else queue.first()
        }

        override suspend fun registerEvent(animalId: String, request: RegisterSanitaryEventRequest) = error("no usado")
        override suspend fun getCalendar(year: Int, month: Int) = error("no usado")
        override suspend fun scheduleEvent(animalId: String, request: ScheduleSanitaryEventRequest) = error("no usado")
        override suspend fun completeEvent(animalId: String, eventId: String, request: CompleteSanitaryEventRequest) = error("no usado")
    }

    private fun ok(events: List<ClinicalHistoryEventDto>, message: String? = null) =
        Response.success(ClinicalHistoryResponse(animalId, events, message))

    private fun badRequest(message: String): Response<ClinicalHistoryResponse> =
        Response.error(400, """{"message":"$message"}""".toResponseBody("application/json".toMediaType()))

    // ---------------- ViewModel ----------------

    @Test
    fun `init carga el animalId recibido`() = runTest(dispatcher) {
        val service = FakeService(ok(listOf(event("e1", SanitaryEventStatus.COMPLETED, "2026-03-01T10:00:00"))))
        val viewModel = ClinicalHistoryViewModel(SanitaryRepository(service), animalId)
        advanceUntilIdle()

        assertEquals(listOf(animalId), service.requested)
        assertEquals(listOf("e1"), viewModel.state.value.data?.map { it.id })
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `historial vacio muestra Sin registros`() = runTest(dispatcher) {
        val service = FakeService(ok(emptyList())) // message null -> fallback
        val viewModel = ClinicalHistoryViewModel(SanitaryRepository(service), animalId)
        advanceUntilIdle()

        assertEquals(emptyList<ClinicalHistoryEventDto>(), viewModel.state.value.data)
        assertEquals("Sin registros.", viewModel.state.value.message)
    }

    @Test
    fun `conserva exactamente el orden recibido del backend`() = runTest(dispatcher) {
        // Orden deliberadamente no alfabetico ni por fecha: Android no debe reordenar
        val events = listOf(
            event("c", SanitaryEventStatus.SCHEDULED, scheduledDate = "2027-01-20"),
            event("a", SanitaryEventStatus.COMPLETED, occurredAt = "2025-12-01T08:30:00"),
            event("b", SanitaryEventStatus.CANCELLED, scheduledDate = "2026-06-01")
        )
        val viewModel = ClinicalHistoryViewModel(SanitaryRepository(FakeService(ok(events))), animalId)
        advanceUntilIdle()

        assertEquals(listOf("c", "a", "b"), viewModel.state.value.data?.map { it.id })
    }

    @Test
    fun `error expone el mensaje del backend`() = runTest(dispatcher) {
        val service = FakeService(badRequest("Parametro invalido: animalId"))
        val viewModel = ClinicalHistoryViewModel(SanitaryRepository(service), "1")
        advanceUntilIdle()

        assertNull(viewModel.state.value.data)
        assertEquals("Parametro invalido: animalId", viewModel.state.value.message)
    }

    @Test
    fun `reintentar vuelve a consultar`() = runTest(dispatcher) {
        val service = FakeService(
            badRequest("Error temporal"),
            ok(listOf(event("e1", SanitaryEventStatus.COMPLETED, "2026-03-01T10:00:00")))
        )
        val viewModel = ClinicalHistoryViewModel(SanitaryRepository(service), animalId)
        advanceUntilIdle()

        viewModel.load()
        advanceUntilIdle()

        assertEquals(listOf(animalId, animalId), service.requested)
        assertEquals(listOf("e1"), viewModel.state.value.data?.map { it.id })
    }

    // ---------------- Fecha efectiva ----------------

    @Test
    fun `COMPLETED usa occurredAt`() = assertEquals(
        LocalDate.of(2025, 12, 1),
        effectiveDate(event("e", SanitaryEventStatus.COMPLETED, occurredAt = "2025-12-01T08:30:00"))
    )

    @Test
    fun `SCHEDULED usa scheduledDate`() = assertEquals(
        LocalDate.of(2027, 1, 20),
        effectiveDate(event("e", SanitaryEventStatus.SCHEDULED, scheduledDate = "2027-01-20"))
    )

    @Test
    fun `CANCELLED usa scheduledDate`() = assertEquals(
        LocalDate.of(2026, 6, 1),
        effectiveDate(event("e", SanitaryEventStatus.CANCELLED, scheduledDate = "2026-06-01"))
    )

    @Test
    fun `COMPLETED con ambas fechas usa occurredAt`() = assertEquals(
        LocalDate.of(2026, 3, 2),
        effectiveDate(
            event("e", SanitaryEventStatus.COMPLETED, occurredAt = "2026-03-02T09:00:00", scheduledDate = "2026-03-01")
        )
    )

    @Test
    fun `sin fecha utilizable no se inventa una`() {
        assertNull(effectiveDate(event("e", SanitaryEventStatus.SCHEDULED)))
        assertNull(effectiveDate(event("e", null, occurredAt = "2026-03-01T10:00:00")))
    }
}

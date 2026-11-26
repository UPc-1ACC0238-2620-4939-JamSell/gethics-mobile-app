package com.jamsell.gethics.sanitary.presentation.upcoming_events

import com.jamsell.gethics.sanitary.data.CompleteSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.RegisterSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.RescheduleSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.sanitary.data.SanitaryService
import com.jamsell.gethics.sanitary.data.ScheduleSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.UpcomingSanitaryEventDto
import com.jamsell.gethics.sanitary.data.UpcomingSanitaryEventsResponse
import com.jamsell.gethics.sanitary.data.repository.CONNECTION_ERROR
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class UpcomingEventsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun dto(id: String, animalId: String, date: String, daysUntil: Long, overdue: Boolean = daysUntil < 0) =
        UpcomingSanitaryEventDto(id, animalId, SanitaryEventType.VACCINATION, date, "Aftosa", daysUntil, overdue)

    private class FakeService(val upcoming: () -> Response<UpcomingSanitaryEventsResponse>) : SanitaryService {
        val requestedDays = mutableListOf<Int>()

        override suspend fun getUpcomingEvents(days: Int): Response<UpcomingSanitaryEventsResponse> {
            requestedDays += days
            return upcoming()
        }

        override suspend fun registerEvent(animalId: String, request: RegisterSanitaryEventRequest) = error("no usado")
        override suspend fun getCalendar(year: Int, month: Int) = error("no usado")
        override suspend fun scheduleEvent(animalId: String, request: ScheduleSanitaryEventRequest) = error("no usado")
        override suspend fun completeEvent(animalId: String, eventId: String, request: CompleteSanitaryEventRequest) = error("no usado")
        override suspend fun cancelEvent(animalId: String, eventId: String) = error("no usado")
        override suspend fun rescheduleEvent(animalId: String, eventId: String, request: RescheduleSanitaryEventRequest) = error("no usado")
        override suspend fun getClinicalHistory(animalId: String) = error("no usado")
    }

    private fun response(vararg events: UpcomingSanitaryEventDto, message: String? = null) =
        Response.success(UpcomingSanitaryEventsResponse("2026-11-25", UPCOMING_WINDOW_DAYS, events.toList(), message))

    @Test
    fun `carga proximos y vencidos con su animal y nivel de urgencia`() = runTest(dispatcher) {
        val service = FakeService {
            response(
                dto("vencido", "a1", "2026-11-23", -2),
                dto("hoy", "a2", "2026-11-25", 0),
                dto("en2", "a1", "2026-11-27", 2),
                dto("en5", "a3", "2026-11-30", 5)
            )
        }
        val viewModel = UpcomingEventsViewModel(SanitaryRepository(service)) { mapOf("a1" to "Lola", "a2" to "TAG-002") }
        advanceUntilIdle()

        val items = viewModel.state.value.data!!
        assertEquals(listOf(UPCOMING_WINDOW_DAYS), service.requestedDays)
        assertEquals(listOf("vencido", "hoy", "en2", "en5"), items.map { it.id })
        assertEquals(
            listOf(EventUrgency.OVERDUE, EventUrgency.URGENT, EventUrgency.URGENT, EventUrgency.UPCOMING),
            items.map { it.urgency }
        )
        assertEquals(listOf("Lola", "TAG-002", "Lola", null), items.map { it.animalLabel })
        assertEquals(LocalDate.of(2026, 11, 23), items.first().scheduledDate)
        assertEquals("", viewModel.state.value.message)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `sin eventos muestra el mensaje del backend y no pide los animales`() = runTest(dispatcher) {
        var animalsRequested = false
        val viewModel = UpcomingEventsViewModel(SanitaryRepository(FakeService { response(message = "No hay alertas pendientes.") })) {
            animalsRequested = true
            emptyMap()
        }
        advanceUntilIdle()

        assertEquals(emptyList<UpcomingEventItem>(), viewModel.state.value.data)
        assertEquals("No hay alertas pendientes.", viewModel.state.value.message)
        assertFalse(animalsRequested)
    }

    @Test
    fun `si fallan los animales la lista se muestra igual sin nombre`() = runTest(dispatcher) {
        val viewModel = UpcomingEventsViewModel(SanitaryRepository(FakeService { response(dto("e", "a1", "2026-11-26", 1)) })) {
            throw IOException("sin red")
        }
        advanceUntilIdle()

        assertNull(viewModel.state.value.data!!.single().animalLabel)
    }

    @Test
    fun `error del backend deja data null con su mensaje`() = runTest(dispatcher) {
        val error = """{"message":"La ventana de alerta debe estar entre 1 y 30 dias."}""".toResponseBody("application/json".toMediaType())
        val viewModel = UpcomingEventsViewModel(SanitaryRepository(FakeService { Response.error(400, error) }))
        advanceUntilIdle()

        assertNull(viewModel.state.value.data)
        assertEquals("La ventana de alerta debe estar entre 1 y 30 dias.", viewModel.state.value.message)
    }

    @Test
    fun `sin conexion muestra el error de conexion y reintentar vuelve a cargar`() = runTest(dispatcher) {
        var fail = true
        val service = FakeService { if (fail) throw IOException("sin red") else response(dto("e", "a1", "2026-11-26", 1)) }
        val viewModel = UpcomingEventsViewModel(SanitaryRepository(service))
        advanceUntilIdle()

        assertNull(viewModel.state.value.data)
        assertEquals(CONNECTION_ERROR, viewModel.state.value.message)

        fail = false
        viewModel.load()
        assertTrue(viewModel.state.value.isLoading)
        advanceUntilIdle()

        assertEquals(listOf("e"), viewModel.state.value.data!!.map { it.id })
    }

    @Test
    fun `urgencia segun los dias restantes`() {
        assertEquals(EventUrgency.OVERDUE, urgencyOf(-1, overdue = true))
        assertEquals(EventUrgency.OVERDUE, urgencyOf(-3, overdue = false))
        assertEquals(EventUrgency.URGENT, urgencyOf(0, overdue = false))
        assertEquals(EventUrgency.URGENT, urgencyOf(URGENT_WITHIN_DAYS.toLong(), overdue = false))
        assertEquals(EventUrgency.UPCOMING, urgencyOf(URGENT_WITHIN_DAYS + 1L, overdue = false))
    }
}

package com.jamsell.gethics.sanitary.presentation.register_event

import com.jamsell.gethics.sanitary.data.RegisterSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.sanitary.data.CompleteSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.ScheduleSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryService
import com.jamsell.gethics.sanitary.data.local.FakePendingSanitaryOperationDao
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterEventViewModelTest {

    private val today = LocalDate.of(2026, 10, 4)

    @Test
    fun `fecha de ayer es valida`() = assertNull(validateEventDate(today.minusDays(1), today))

    @Test
    fun `fecha de hoy es valida`() = assertNull(validateEventDate(today, today))

    @Test
    fun `fecha de manana es invalida`() = assertEquals(FUTURE_DATE_ERROR, validateEventDate(today.plusDays(1), today))

    @Test
    fun `occurredAt se envia como LocalDateTime ISO con segundos`() =
        assertEquals("2026-10-04T00:00:00", toOccurredAt(today))

    @Test
    fun `today 2026-10-04 y fecha 2026-10-05 muestra el mensaje y no llama al backend`() {
        // Instante de la prueba manual: 2026-10-05 00:27 UTC = 2026-10-04 19:27 en Lima
        val clock = Clock.fixed(Instant.parse("2026-10-05T00:27:00Z"), ZoneId.of("America/Lima"))
        val service = CountingService()
        val viewModel = RegisterEventViewModel(SanitaryRepository(service), clock)

        viewModel.save("animal-uuid", SanitaryEventType.VACCINATION, LocalDate.of(2026, 10, 5), "")

        assertEquals(FUTURE_DATE_ERROR, viewModel.state.value.message)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(0, service.calls)
    }

    // ---------------- US35: guardado online (SENT) y sin conexion (QUEUED) ----------------

    private val dispatcher = StandardTestDispatcher()
    private val lima = Clock.fixed(Instant.parse("2026-10-04T15:00:00Z"), ZoneId.of("America/Lima"))

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `guardado online - success y savedOffline false`() = runTest(dispatcher) {
        val service = CountingService()
        val viewModel = RegisterEventViewModel(SanitaryRepository(service, FakePendingSanitaryOperationDao()), lima)

        viewModel.save("animal-uuid", SanitaryEventType.TREATMENT, LocalDate.of(2026, 10, 3), "  Ivermectina  ")
        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.data)
        assertFalse(viewModel.savedOffline.value)
        val request = service.requests.single()
        assertEquals(RegisterSanitaryEventRequest(SanitaryEventType.TREATMENT, "2026-10-03T00:00:00", "Ivermectina", request.clientRequestId), request)
    }

    @Test
    fun `sin conexion con cola offline - success, savedOffline true y queda en la cola`() = runTest(dispatcher) {
        val dao = FakePendingSanitaryOperationDao()
        val service = CountingService { throw IOException() }
        val viewModel = RegisterEventViewModel(SanitaryRepository(service, dao), lima)

        viewModel.save("animal-uuid", SanitaryEventType.TREATMENT, LocalDate.of(2026, 10, 3), "")
        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.data) // vuelve atras como un guardado normal, no es un error
        assertEquals("", viewModel.state.value.message)
        assertTrue(viewModel.savedOffline.value)
        assertEquals(service.requests.single().clientRequestId, dao.rows.single().clientRequestId)
    }

    @Test
    fun `error del backend no marca savedOffline y muestra su message`() = runTest(dispatcher) {
        val service = CountingService {
            Response.error(400, """{"message":"La fecha del evento no puede ser posterior a hoy."}""".toResponseBody("application/json".toMediaType()))
        }
        val viewModel = RegisterEventViewModel(SanitaryRepository(service, FakePendingSanitaryOperationDao()), lima)

        viewModel.save("animal-uuid", SanitaryEventType.TREATMENT, LocalDate.of(2026, 10, 3), "")
        advanceUntilIdle()

        assertNull(viewModel.state.value.data)
        assertFalse(viewModel.savedOffline.value)
        assertEquals("La fecha del evento no puede ser posterior a hoy.", viewModel.state.value.message)
    }

    private class CountingService(val response: () -> Response<Unit> = { Response.success(201, Unit) }) : SanitaryService {
        var calls = 0
        val requests = mutableListOf<RegisterSanitaryEventRequest>()
        override suspend fun registerEvent(animalId: String, request: RegisterSanitaryEventRequest): Response<Unit> {
            calls++
            requests += request
            return response()
        }

        override suspend fun getCalendar(year: Int, month: Int) = error("no usado")

        override suspend fun getClinicalHistory(animalId: String) = error("no usado")
        override suspend fun scheduleEvent(animalId: String, request: ScheduleSanitaryEventRequest) = error("no usado")
        override suspend fun completeEvent(animalId: String, eventId: String, request: CompleteSanitaryEventRequest) = error("no usado")
        override suspend fun cancelEvent(animalId: String, eventId: String) = error("no usado")
    }
}

package com.jamsell.gethics.sanitary.presentation.schedule_vaccination

import com.jamsell.gethics.sanitary.data.CompleteSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.RegisterSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.sanitary.data.SanitaryService
import com.jamsell.gethics.sanitary.data.ScheduleSanitaryEventRequest
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
class ScheduleVaccinationViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // 2026-10-04 en Lima
    private val clock = Clock.fixed(Instant.parse("2026-10-04T15:00:00Z"), ZoneId.of("America/Lima"))

    private class FakeService(val response: () -> Response<Unit> = { Response.success(201, Unit) }) : SanitaryService {
        val requests = mutableListOf<Pair<String, ScheduleSanitaryEventRequest>>()

        override suspend fun scheduleEvent(animalId: String, request: ScheduleSanitaryEventRequest): Response<Unit> {
            requests += animalId to request
            return response()
        }

        override suspend fun registerEvent(animalId: String, request: RegisterSanitaryEventRequest) = error("no usado")
        override suspend fun getCalendar(year: Int, month: Int) = error("no usado")
        override suspend fun getClinicalHistory(animalId: String) = error("no usado")
        override suspend fun completeEvent(animalId: String, eventId: String, request: CompleteSanitaryEventRequest) = error("no usado")
    }

    @Test
    fun `programa VACCINATION con la fecha ISO y la descripcion recortada`() = runTest(dispatcher) {
        val service = FakeService()
        val viewModel = ScheduleVaccinationViewModel(SanitaryRepository(service), clock)

        viewModel.schedule("animal-uuid", LocalDate.of(2026, 10, 7), "  Aftosa  ")
        assertTrue(viewModel.state.value.isLoading)
        advanceUntilIdle()

        assertEquals(listOf("animal-uuid" to ScheduleSanitaryEventRequest(SanitaryEventType.VACCINATION, "2026-10-07", "Aftosa")), service.requests)
        assertEquals(true, viewModel.state.value.data)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `descripcion vacia se envia como null y hoy es una fecha valida`() = runTest(dispatcher) {
        val service = FakeService()
        val viewModel = ScheduleVaccinationViewModel(SanitaryRepository(service), clock)

        viewModel.schedule("animal-uuid", LocalDate.of(2026, 10, 4), "   ")
        advanceUntilIdle()

        val request = service.requests.single().second
        assertEquals("2026-10-04", request.scheduledDate)
        assertNull(request.description)
    }

    @Test
    fun `fecha anterior a hoy muestra el mensaje y no llama al backend`() = runTest(dispatcher) {
        val service = FakeService()
        val viewModel = ScheduleVaccinationViewModel(SanitaryRepository(service), clock)

        viewModel.schedule("animal-uuid", LocalDate.of(2026, 10, 3), "")
        advanceUntilIdle()

        assertEquals(PAST_SCHEDULED_DATE_ERROR, viewModel.state.value.message)
        assertTrue(service.requests.isEmpty())
    }

    @Test
    fun `error del backend expone su message`() = runTest(dispatcher) {
        val service = FakeService {
            Response.error(400, """{"message":"La descripcion no puede superar 1000 caracteres."}""".toResponseBody("application/json".toMediaType()))
        }
        val viewModel = ScheduleVaccinationViewModel(SanitaryRepository(service), clock)

        viewModel.schedule("animal-uuid", LocalDate.of(2026, 10, 7), "x")
        advanceUntilIdle()

        assertNull(viewModel.state.value.data)
        assertEquals("La descripcion no puede superar 1000 caracteres.", viewModel.state.value.message)
    }

    @Test
    fun `sin conexion muestra el error de conexion`() = runTest(dispatcher) {
        val viewModel = ScheduleVaccinationViewModel(SanitaryRepository(FakeService { throw IOException() }), clock)

        viewModel.schedule("animal-uuid", LocalDate.of(2026, 10, 7), "")
        advanceUntilIdle()

        assertEquals("No se pudo conectar con el servidor", viewModel.state.value.message)
    }
}

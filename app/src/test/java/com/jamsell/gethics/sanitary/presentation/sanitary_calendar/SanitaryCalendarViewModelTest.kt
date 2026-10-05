package com.jamsell.gethics.sanitary.presentation.sanitary_calendar

import com.jamsell.gethics.sanitary.data.RegisterSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryCalendarResponse
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.sanitary.data.SanitaryService
import com.jamsell.gethics.sanitary.data.ScheduledEventDto
import com.jamsell.gethics.sanitary.data.repository.SanitaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class SanitaryCalendarViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun clockAt(instant: String) = Clock.fixed(Instant.parse(instant), ZoneId.of("America/Lima"))

    private fun event(id: String, date: String) =
        ScheduledEventDto(id, "animal-uuid", SanitaryEventType.CHECKUP, date, null, "SCHEDULED")

    /** Devuelve un evento por mes pedido (id = "año-mes"); delayMs permite simular respuestas lentas. */
    private class FakeService(
        val delayMs: (YearMonth) -> Long = { 0 },
        val events: (YearMonth) -> List<ScheduledEventDto>
    ) : SanitaryService {
        val requested = mutableListOf<YearMonth>()

        override suspend fun getCalendar(year: Int, month: Int): Response<SanitaryCalendarResponse> {
            val period = YearMonth.of(year, month)
            requested += period
            delay(delayMs(period))
            return Response.success(SanitaryCalendarResponse(year, month, events(period), null))
        }

        override suspend fun registerEvent(animalId: String, request: RegisterSanitaryEventRequest) = error("no usado")

        override suspend fun getClinicalHistory(animalId: String) = error("no usado")
    }

    @Test
    fun `al iniciar carga el mes actual`() = runTest(dispatcher) {
        // 2026-10-05 00:27 UTC = 2026-10-04 en Lima -> octubre
        val service = FakeService { listOf(event("$it", "2026-10-15")) }
        val viewModel = SanitaryCalendarViewModel(SanitaryRepository(service), clockAt("2026-10-05T00:27:00Z"))
        advanceUntilIdle()

        assertEquals(YearMonth.of(2026, 10), viewModel.selectedMonth.value)
        assertEquals(listOf(YearMonth.of(2026, 10)), service.requested)
        assertEquals(listOf("2026-10"), viewModel.state.value.data?.map { it.id })
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `diciembre 2026 nextMonth pide enero 2027`() = runTest(dispatcher) {
        val service = FakeService { emptyList() }
        val viewModel = SanitaryCalendarViewModel(SanitaryRepository(service), clockAt("2026-12-15T12:00:00Z"))
        advanceUntilIdle()

        viewModel.nextMonth()
        advanceUntilIdle()

        assertEquals(YearMonth.of(2027, 1), viewModel.selectedMonth.value)
        assertEquals(YearMonth.of(2027, 1), service.requested.last())
    }

    @Test
    fun `enero 2027 previousMonth pide diciembre 2026`() = runTest(dispatcher) {
        val service = FakeService { emptyList() }
        val viewModel = SanitaryCalendarViewModel(SanitaryRepository(service), clockAt("2027-01-15T12:00:00Z"))
        advanceUntilIdle()

        viewModel.previousMonth()
        advanceUntilIdle()

        assertEquals(YearMonth.of(2026, 12), viewModel.selectedMonth.value)
        assertEquals(YearMonth.of(2026, 12), service.requested.last())
    }

    @Test
    fun `respuesta vacia muestra No hay actividades pendientes`() = runTest(dispatcher) {
        val service = FakeService { emptyList() } // message null -> fallback
        val viewModel = SanitaryCalendarViewModel(SanitaryRepository(service), clockAt("2026-11-10T12:00:00Z"))
        advanceUntilIdle()

        assertEquals(emptyList<ScheduledEventDto>(), viewModel.state.value.data)
        assertEquals("No hay actividades pendientes.", viewModel.state.value.message)
    }

    @Test
    fun `cambio rapido de mes conserva la respuesta mas reciente`() = runTest(dispatcher) {
        // octubre tarda mucho, noviembre responde enseguida: sin cancelar, octubre pisaria a noviembre
        val october = YearMonth.of(2026, 10)
        val service = FakeService(delayMs = { if (it == october) 5_000 else 10 }) { listOf(event("$it", "${it}-01")) }
        val viewModel = SanitaryCalendarViewModel(SanitaryRepository(service), clockAt("2026-10-10T12:00:00Z"))
        runCurrent() // la peticion de octubre ya esta en curso

        viewModel.nextMonth()
        advanceUntilIdle()

        assertEquals(listOf(october, YearMonth.of(2026, 11)), service.requested)
        assertEquals(YearMonth.of(2026, 11), viewModel.selectedMonth.value)
        assertEquals(listOf("2026-11"), viewModel.state.value.data?.map { it.id })
    }
}

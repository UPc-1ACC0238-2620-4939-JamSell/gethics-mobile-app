package com.jamsell.gethics.sanitary.presentation.register_event

import com.jamsell.gethics.sanitary.data.RegisterSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.sanitary.data.CompleteSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.ScheduleSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryService
import com.jamsell.gethics.sanitary.data.repository.SanitaryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

    private class CountingService : SanitaryService {
        var calls = 0
        override suspend fun registerEvent(animalId: String, request: RegisterSanitaryEventRequest): Response<Unit> {
            calls++
            return Response.success(201, Unit)
        }

        override suspend fun getCalendar(year: Int, month: Int) = error("no usado")

        override suspend fun getClinicalHistory(animalId: String) = error("no usado")
        override suspend fun scheduleEvent(animalId: String, request: ScheduleSanitaryEventRequest) = error("no usado")
        override suspend fun completeEvent(animalId: String, eventId: String, request: CompleteSanitaryEventRequest) = error("no usado")
    }
}

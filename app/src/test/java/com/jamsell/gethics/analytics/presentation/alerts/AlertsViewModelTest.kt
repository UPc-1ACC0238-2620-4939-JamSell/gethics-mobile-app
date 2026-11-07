package com.jamsell.gethics.analytics.presentation.alerts

import com.jamsell.gethics.analytics.data.AlertDto
import com.jamsell.gethics.analytics.data.AlertListResponse
import com.jamsell.gethics.analytics.data.AlertService
import com.jamsell.gethics.analytics.data.AlertStatus
import com.jamsell.gethics.analytics.data.RiskLevel
import com.jamsell.gethics.analytics.data.TrendType
import com.jamsell.gethics.analytics.data.repository.AlertRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class AlertsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val ownerId = "11111111-1111-1111-1111-111111111111"

    private fun alert(id: String) = AlertDto(
        id, "trend-$id", TrendType.SANITARY, RiskLevel.HIGH, "mensaje $id", AlertStatus.PENDING, "2026-11-01T10:00:00Z"
    )

    /** Responde con la lista de respuestas en orden (la ultima se repite) y registra los ownerId pedidos. */
    private class FakeService(vararg responses: Response<AlertListResponse>) : AlertService {
        private val queue = responses.toMutableList()
        val requested = mutableListOf<String>()

        override suspend fun getAlerts(ownerId: String): Response<AlertListResponse> {
            requested += ownerId
            return if (queue.size > 1) queue.removeAt(0) else queue.first()
        }
    }

    private fun ok(alerts: List<AlertDto>, message: String? = null) = Response.success(AlertListResponse(alerts, message))

    private fun badRequest(message: String): Response<AlertListResponse> =
        Response.error(400, """{"message":"$message"}""".toResponseBody("application/json".toMediaType()))

    @Test
    fun `init pasa de loading a success con las alertas del ownerId recibido`() = runTest(dispatcher) {
        val service = FakeService(ok(listOf(alert("a2"), alert("a1"))))
        val viewModel = AlertsViewModel(AlertRepository(service), ownerId)

        assertTrue(viewModel.state.value.isLoading)

        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(listOf(ownerId), service.requested)
        assertEquals(listOf("a2", "a1"), viewModel.state.value.data?.map { it.id }) // orden del backend
        assertEquals("", viewModel.state.value.message)
    }

    @Test
    fun `lista vacia muestra el message del backend`() = runTest(dispatcher) {
        val viewModel = AlertsViewModel(AlertRepository(FakeService(ok(emptyList(), "No hay alertas registradas."))), ownerId)
        advanceUntilIdle()

        assertEquals(emptyList<AlertDto>(), viewModel.state.value.data)
        assertEquals("No hay alertas registradas.", viewModel.state.value.message)
    }

    @Test
    fun `lista vacia sin message usa el fallback`() = runTest(dispatcher) {
        val viewModel = AlertsViewModel(AlertRepository(FakeService(ok(emptyList(), null))), ownerId)
        advanceUntilIdle()

        assertEquals(emptyList<AlertDto>(), viewModel.state.value.data)
        assertEquals(NO_ALERTS, viewModel.state.value.message)
    }

    @Test
    fun `error deja data en null y muestra el mensaje`() = runTest(dispatcher) {
        val viewModel = AlertsViewModel(AlertRepository(FakeService(badRequest("Parametro invalido: ownerId"))), ownerId)
        advanceUntilIdle()

        assertNull(viewModel.state.value.data)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Parametro invalido: ownerId", viewModel.state.value.message)
    }

    @Test
    fun `reintentar tras un error vuelve a pedir y muestra las alertas`() = runTest(dispatcher) {
        val service = FakeService(badRequest("fallo"), ok(listOf(alert("a1"))))
        val viewModel = AlertsViewModel(AlertRepository(service), ownerId)
        advanceUntilIdle()
        assertNull(viewModel.state.value.data)

        viewModel.load()
        assertTrue(viewModel.state.value.isLoading)
        advanceUntilIdle()

        assertEquals(listOf(ownerId, ownerId), service.requested)
        assertEquals(listOf("a1"), viewModel.state.value.data?.map { it.id })
    }
}

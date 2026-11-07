package com.jamsell.gethics.analytics.data.repository

import com.jamsell.gethics.analytics.data.AlertDto
import com.jamsell.gethics.analytics.data.AlertListResponse
import com.jamsell.gethics.analytics.data.AlertService
import com.jamsell.gethics.analytics.data.AlertStatus
import com.jamsell.gethics.analytics.data.RiskLevel
import com.jamsell.gethics.analytics.data.TrendType
import com.jamsell.gethics.shared.common.Resource
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AlertRepositoryTest {

    private val ownerId = "11111111-1111-1111-1111-111111111111"

    /** Responde lo que indique el test y registra los ownerId pedidos. */
    private class FakeService(val respond: () -> Response<AlertListResponse>) : AlertService {
        val requested = mutableListOf<String>()

        override suspend fun getAlerts(ownerId: String): Response<AlertListResponse> {
            requested += ownerId
            return respond()
        }
    }

    private val alert = AlertDto(
        id = "cccccccc-0000-0000-0000-000000000002",
        trendId = "bbbbbbbb-0000-0000-0000-000000000002",
        trendType = TrendType.FINANCIAL,
        riskLevel = RiskLevel.CRITICAL,
        message = "Alerta financiera",
        status = AlertStatus.PENDING,
        createdAt = "2026-11-02T10:00:00Z"
    )

    private fun <T> error(code: Int, body: String): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    @Test
    fun `200 con alertas devuelve Success con la lista`() = runBlocking {
        val result = AlertRepository(FakeService { Response.success(AlertListResponse(listOf(alert), null)) }).getAlerts(ownerId)

        assertTrue(result is Resource.Success)
        assertEquals(listOf(alert), result.data?.alerts)
    }

    @Test
    fun `200 vacio devuelve Success con el message del backend`() = runBlocking {
        val body = AlertListResponse(emptyList(), "No hay alertas registradas.")
        val result = AlertRepository(FakeService { Response.success(body) }).getAlerts(ownerId)

        assertTrue(result is Resource.Success)
        assertTrue(result.data!!.alerts.isEmpty())
        assertEquals("No hay alertas registradas.", result.data!!.message)
    }

    @Test
    fun `400 devuelve el message del backend`() = runBlocking {
        val result = AlertRepository(FakeService { error(400, """{"message":"Parametro invalido: ownerId"}""") })
            .getAlerts("no-es-un-uuid")

        assertTrue(result is Resource.Error)
        assertEquals("Parametro invalido: ownerId", result.message)
    }

    @Test
    fun `500 sin body parseable devuelve mensaje generico con el codigo`() = runBlocking {
        val result = AlertRepository(FakeService { error(500, "<html>oops</html>") }).getAlerts(ownerId)

        assertEquals("No se pudo completar la solicitud (HTTP 500)", result.message)
    }

    @Test
    fun `sin conexion devuelve error de conexion`() = runBlocking {
        val result = AlertRepository(FakeService { throw IOException() }).getAlerts(ownerId)

        assertTrue(result is Resource.Error)
        assertEquals("No se pudo conectar con el servidor", result.message)
    }

    @Test
    fun `envia al service el ownerId recibido`() = runBlocking {
        val service = FakeService { Response.success(AlertListResponse(emptyList(), null)) }
        AlertRepository(service).getAlerts(ownerId)

        assertEquals(listOf(ownerId), service.requested)
    }
}

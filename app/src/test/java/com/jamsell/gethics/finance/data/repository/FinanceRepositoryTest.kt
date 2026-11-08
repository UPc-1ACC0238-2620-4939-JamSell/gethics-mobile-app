package com.jamsell.gethics.finance.data.repository

import com.jamsell.gethics.finance.data.FinanceService
import com.jamsell.gethics.finance.data.FinancialSummaryResponse
import com.jamsell.gethics.finance.data.FinancialTransactionType
import com.jamsell.gethics.finance.data.RegisterTransactionRequest
import com.jamsell.gethics.finance.data.TransactionResponse
import com.jamsell.gethics.shared.common.Resource
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

class FinanceRepositoryTest {

    private val summary = FinancialSummaryResponse("owner", 100.0, 40.0, 60.0, emptyList(), null)

    private class FakeService(
        val summaryResult: () -> Response<FinancialSummaryResponse>,
        val registerResult: () -> Response<TransactionResponse> = { error("no usado") }
    ) : FinanceService {
        var lastFrom: String? = "sin llamar"
        var lastTo: String? = "sin llamar"

        override suspend fun registerTransaction(request: RegisterTransactionRequest) = registerResult()

        override suspend fun getSummary(ownerId: String, from: String?, to: String?): Response<FinancialSummaryResponse> {
            lastFrom = from
            lastTo = to
            return summaryResult()
        }
    }

    @Test
    fun `getSummary envia el rango como yyyy-MM-dd y devuelve el resumen`() = runBlocking {
        val service = FakeService({ Response.success(summary) })
        val result = FinanceRepository(service).getSummary("owner", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))

        assertEquals("2026-11-01", service.lastFrom)
        assertEquals("2026-11-30", service.lastTo)
        assertEquals(60.0, (result as Resource.Success).data!!.balance, 0.0)
    }

    @Test
    fun `getSummary sin periodo no envia fechas`() = runBlocking {
        val service = FakeService({ Response.success(summary) })
        FinanceRepository(service).getSummary("owner", null, null)

        assertEquals(null, service.lastFrom)
        assertEquals(null, service.lastTo)
    }

    @Test
    fun `getSummary devuelve el message del backend en un 400`() = runBlocking {
        val body = """{"message":"Both 'from' and 'to' must be provided"}""".toResponseBody("application/json".toMediaType())
        val service = FakeService({ Response.error(400, body) })
        val result = FinanceRepository(service).getSummary("owner", null, null)

        assertEquals("Both 'from' and 'to' must be provided", (result as Resource.Error).message)
    }

    @Test
    fun `registerTransaction sin red devuelve error de conexion`() = runBlocking {
        val service = FakeService({ error("no usado") }, { throw IOException("sin red") })
        val request = RegisterTransactionRequest("owner", FinancialTransactionType.EXPENSE, 10.0, "Alimento", "2026-11-06", null)
        val result = FinanceRepository(service).registerTransaction(request)

        assertTrue(result is Resource.Error)
        assertEquals("No se pudo conectar con el servidor", (result as Resource.Error).message)
    }
}

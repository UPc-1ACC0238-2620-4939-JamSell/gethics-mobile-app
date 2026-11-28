package com.jamsell.gethics.analytics.presentation

import com.jamsell.gethics.analytics.data.AnalyticsService
import com.jamsell.gethics.analytics.data.FinanceReportSectionResponse
import com.jamsell.gethics.analytics.data.LivestockReportResponse
import com.jamsell.gethics.analytics.data.ReportSectionResponse
import com.jamsell.gethics.analytics.data.repository.AnalyticsRepository
import com.jamsell.gethics.analytics.presentation.reports.ReportFilters
import com.jamsell.gethics.analytics.presentation.reports.ReportsViewModel
import com.jamsell.gethics.finance.data.FinancialTransactionType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelFiltersTest {

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val owner = "00000000-0000-0000-0000-000000000001"
    private val mar1 = LocalDate.of(2026, 3, 1)
    private val mar31 = LocalDate.of(2026, 3, 31)

    /** Lo que llego al backend. type y category son los strings de la query (null = no se envian). */
    private data class Call(val ownerId: String, val from: String, val to: String, val type: String?, val category: String?)

    private class FakeService : AnalyticsService {
        val reportCalls = mutableListOf<Call>()
        val exportCalls = mutableListOf<Call>()
        var reportHandler: suspend (Call) -> Response<LivestockReportResponse> = { call -> report(call.category ?: "todas") }
        var exportHandler: suspend (Call) -> Response<ResponseBody> = { _ -> csv() }

        override suspend fun getReport(ownerId: String, from: String, to: String, type: String?, category: String?): Response<LivestockReportResponse> {
            val call = Call(ownerId, from, to, type, category)
            reportCalls += call
            return reportHandler(call)
        }

        override suspend fun exportReport(ownerId: String, from: String, to: String, type: String?, category: String?): Response<ResponseBody> {
            val call = Call(ownerId, from, to, type, category)
            exportCalls += call
            return exportHandler(call)
        }
    }

    private companion object {
        /** El tag viaja en finance.message para saber a que consulta corresponde el reporte mostrado. */
        fun report(tag: String): Response<LivestockReportResponse> = Response.success(
            LivestockReportResponse(
                ownerId = "o", from = "2026-03-01", to = "2026-03-31",
                finance = FinanceReportSectionResponse(0.0, 10.0, -10.0, false, tag),
                health = ReportSectionResponse(true, "m"),
                productivity = ReportSectionResponse(true, "m")
            )
        )

        fun csv(): Response<ResponseBody> = Response.success(
            "a,b\r\n".toResponseBody("text/csv".toMediaType()),
            Headers.headersOf("Content-Disposition", "attachment; filename=\"reporte-ganado-2026-03-01-2026-03-31.csv\"")
        )

        fun <T> error(code: Int, message: String): Response<T> =
            Response.error(code, """{"message":"$message"}""".toResponseBody("application/json".toMediaType()))
    }

    private fun viewModel(service: FakeService): ReportsViewModel {
        val vm = ReportsViewModel(AnalyticsRepository(service), owner)
        vm.setFrom(mar1)
        vm.setTo(mar31)
        return vm
    }

    /** El repositorio lee el CSV en Dispatchers.IO (hilo real): se espera a que la exportacion deje de estar en curso. */
    private fun kotlinx.coroutines.test.TestScope.awaitExport(vm: ReportsViewModel) {
        val deadline = System.currentTimeMillis() + 5_000
        do {
            advanceUntilIdle()
            if (!vm.exportState.value.isLoading) return
            Thread.sleep(10)
        } while (System.currentTimeMillis() < deadline)
        throw AssertionError("la exportacion no termino")
    }

    private fun call(type: String? = null, category: String? = null) = Call(owner, "2026-03-01", "2026-03-31", type, category)

    @Test
    fun `estado inicial sin filtros aplicados y sin consultas`() {
        val service = FakeService()
        val vm = viewModel(service)

        assertNull(vm.type.value)
        assertEquals("", vm.category.value)
        assertNull(vm.appliedFilters.value)
        assertTrue(service.reportCalls.isEmpty())
        assertNull(vm.exportedFileName.value)
    }

    @Test
    fun `la carga inicial mantiene el comportamiento anterior sin type ni category`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        vm.applyFilters()
        advanceUntilIdle()

        assertEquals(listOf(call()), service.reportCalls)
        assertEquals(ReportFilters(mar1, mar31), vm.appliedFilters.value)
        assertFalse(vm.appliedFilters.value!!.hasActiveFilters)
        assertNotNull(vm.state.value.data)
    }

    @Test
    fun `aplicar type consulta con ese type`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        vm.onTypeChange(FinancialTransactionType.INCOME)
        vm.applyFilters()
        advanceUntilIdle()

        assertEquals(listOf(call(type = "INCOME")), service.reportCalls)
        assertTrue(vm.appliedFilters.value!!.hasActiveFilters)
    }

    @Test
    fun `aplicar category consulta con la categoria recortada y sin cambiar mayusculas`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        vm.onCategoryChange("  AlImento ")
        vm.applyFilters()
        advanceUntilIdle()

        assertEquals(listOf(call(category = "AlImento")), service.reportCalls)
    }

    @Test
    fun `una categoria en blanco equivale a no filtrar`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        vm.onCategoryChange("    ")
        vm.applyFilters()
        advanceUntilIdle()

        assertEquals(listOf(call()), service.reportCalls)
        assertNull(vm.appliedFilters.value!!.category)
    }

    @Test
    fun `filtros combinados se aplican juntos`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        vm.onTypeChange(FinancialTransactionType.EXPENSE)
        vm.onCategoryChange("Alimento")
        vm.applyFilters()
        advanceUntilIdle()

        assertEquals(listOf(call("EXPENSE", "Alimento")), service.reportCalls)
    }

    @Test
    fun `limpiar quita type y category, conserva las fechas aplicadas y vuelve a consultar`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        vm.onTypeChange(FinancialTransactionType.EXPENSE)
        vm.onCategoryChange("Alimento")
        vm.applyFilters()
        advanceUntilIdle()
        vm.setFrom(LocalDate.of(2026, 3, 10)) // borrador de fecha sin aplicar

        vm.clearFilters()
        advanceUntilIdle()

        assertNull(vm.type.value)
        assertEquals("", vm.category.value)
        assertEquals(ReportFilters(mar1, mar31), vm.appliedFilters.value)
        assertEquals(call(), service.reportCalls.last())
        assertEquals(2, service.reportCalls.size)
    }

    @Test
    fun `limpiar antes de la primera consulta consulta sin filtros`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        vm.onCategoryChange("x")
        vm.clearFilters()
        advanceUntilIdle()

        assertEquals(listOf(call()), service.reportCalls)
    }

    @Test
    fun `cambiar controles sin pulsar Aplicar no consulta ni cambia los filtros aplicados`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        vm.onCategoryChange("Alimento")
        vm.applyFilters()
        advanceUntilIdle()

        vm.onCategoryChange("Veterinaria")
        vm.onTypeChange(FinancialTransactionType.INCOME)
        vm.setTo(LocalDate.of(2026, 3, 15))
        advanceUntilIdle()

        assertEquals(1, service.reportCalls.size)
        assertEquals("Alimento", vm.appliedFilters.value!!.category)
        assertNull(vm.appliedFilters.value!!.type)
        assertEquals(mar31, vm.appliedFilters.value!!.to)
    }

    @Test
    fun `exportar usa los filtros aplicados y no el borrador`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        vm.onTypeChange(FinancialTransactionType.EXPENSE)
        vm.onCategoryChange("Alimento")
        vm.applyFilters()
        advanceUntilIdle()

        // El usuario edita el borrador pero no pulsa Aplicar
        vm.onCategoryChange("Veterinaria")
        vm.onTypeChange(null)
        vm.setFrom(LocalDate.of(2026, 3, 20))
        vm.exportReport()
        awaitExport(vm)

        assertEquals(listOf(call("EXPENSE", "Alimento")), service.exportCalls)
    }

    @Test
    fun `despues de Aplicar la exportacion usa los filtros nuevos`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        vm.onCategoryChange("Alimento")
        vm.applyFilters()
        advanceUntilIdle()
        vm.onCategoryChange("Veterinaria")
        vm.applyFilters()
        advanceUntilIdle()
        vm.exportReport()
        awaitExport(vm)

        assertEquals(listOf(call(category = "Veterinaria")), service.exportCalls)
    }

    @Test
    fun `la exportacion lleva exactamente los mismos parametros que el reporte visible`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        vm.onTypeChange(FinancialTransactionType.INCOME)
        vm.onCategoryChange(" Venta ")
        vm.applyFilters()
        advanceUntilIdle()
        vm.exportReport()
        awaitExport(vm)

        assertEquals(service.reportCalls.single(), service.exportCalls.single())
    }

    @Test
    fun `exportar sin reporte aplicado no hace ninguna solicitud`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        vm.exportReport()
        awaitExport(vm)

        assertTrue(service.exportCalls.isEmpty())
    }

    @Test
    fun `un rango invertido no llega al backend y no cambia lo aplicado`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        vm.applyFilters()
        advanceUntilIdle()
        vm.setFrom(LocalDate.of(2026, 4, 1))
        vm.applyFilters()
        advanceUntilIdle()

        assertTrue(vm.rangeError.value)
        assertEquals(1, service.reportCalls.size)
        assertEquals(mar1, vm.appliedFilters.value!!.from)
    }

    @Test
    fun `un error muestra el mensaje y aplicar de nuevo reintenta`() = runTest {
        val service = FakeService()
        service.reportHandler = { error(400, "Parametro invalido: type") }
        val vm = viewModel(service)
        vm.applyFilters()
        advanceUntilIdle()

        assertNull(vm.state.value.data)
        assertEquals("Parametro invalido: type", vm.state.value.message)

        service.reportHandler = { report("ok") }
        vm.applyFilters()
        advanceUntilIdle()

        assertEquals("ok", vm.state.value.data!!.finance.message)
        assertEquals("", vm.state.value.message)
    }

    @Test
    fun `un fallo de red no rompe la pantalla`() = runTest {
        val service = FakeService()
        service.reportHandler = { throw IOException("sin red") }
        val vm = viewModel(service)
        vm.applyFilters()
        advanceUntilIdle()

        assertNull(vm.state.value.data)
        assertTrue(vm.state.value.message.isNotEmpty())
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `aplicar dos veces los mismos filtros mientras carga no duplica la solicitud`() = runTest {
        val service = FakeService()
        val gate = CompletableDeferred<Unit>()
        service.reportHandler = { gate.await(); report("ok") }
        val vm = viewModel(service)
        vm.onCategoryChange("Alimento")
        vm.applyFilters()
        advanceUntilIdle()
        vm.applyFilters()
        vm.applyFilters()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, service.reportCalls.size)
        assertEquals("ok", vm.state.value.data!!.finance.message)
    }

    @Test
    fun `la respuesta de unos filtros anteriores se descarta si se aplicaron otros`() = runTest {
        val service = FakeService()
        val gateOld = CompletableDeferred<Unit>()
        service.reportHandler = { call ->
            if (call.category == "Vieja") gateOld.await()
            report(call.category.orEmpty())
        }
        val vm = viewModel(service)
        vm.onCategoryChange("Vieja")
        vm.applyFilters()
        advanceUntilIdle()
        vm.onCategoryChange("Nueva")
        vm.applyFilters()
        advanceUntilIdle()
        gateOld.complete(Unit)
        advanceUntilIdle()

        assertEquals("Nueva", vm.state.value.data!!.finance.message)
        assertEquals("Nueva", vm.appliedFilters.value!!.category)
    }

    @Test
    fun `una exportacion en curso se descarta si se aplican otros filtros`() = runTest {
        val service = FakeService()
        val gate = CompletableDeferred<Unit>()
        service.exportHandler = { gate.await(); csv() }
        val vm = viewModel(service)
        vm.onCategoryChange("Alimento")
        vm.applyFilters()
        advanceUntilIdle()
        vm.exportReport()
        advanceUntilIdle() // la descarga queda esperando el gate: sigue en curso
        assertTrue(vm.exportState.value.isLoading)

        vm.onCategoryChange("Veterinaria")
        vm.applyFilters()
        gate.complete(Unit)
        awaitExport(vm)

        assertNull(vm.exportState.value.data) // el CSV viejo no se entrega como si fuera del reporte nuevo
        assertFalse(vm.exportState.value.isLoading)
    }

    @Test
    fun `presionar Exportar dos veces no duplica la descarga`() = runTest {
        val service = FakeService()
        val gate = CompletableDeferred<Unit>()
        service.exportHandler = { gate.await(); csv() }
        val vm = viewModel(service)
        vm.applyFilters()
        advanceUntilIdle()
        vm.exportReport()
        vm.exportReport()
        gate.complete(Unit)
        awaitExport(vm)

        assertEquals(1, service.exportCalls.size)
    }

    @Test
    fun `exportacion exitosa entrega el archivo y consumirla guarda su nombre`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        vm.applyFilters()
        advanceUntilIdle()
        vm.exportReport()
        awaitExport(vm)

        assertEquals("reporte-ganado-2026-03-01-2026-03-31.csv", vm.exportState.value.data!!.fileName)
        vm.consumeExport()
        assertNull(vm.exportState.value.data)
        assertEquals("reporte-ganado-2026-03-01-2026-03-31.csv", vm.exportedFileName.value)

        vm.applyFilters() // un reporte nuevo borra el aviso de exportado
        advanceUntilIdle()
        assertNull(vm.exportedFileName.value)
    }

    @Test
    fun `un 404 de exportacion se informa con el mensaje del backend`() = runTest {
        val service = FakeService()
        service.exportHandler = { error(404, "No hay información disponible para el periodo seleccionado") }
        val vm = viewModel(service)
        vm.onTypeChange(FinancialTransactionType.INCOME)
        vm.applyFilters()
        advanceUntilIdle()
        vm.exportReport()
        awaitExport(vm)

        assertNull(vm.exportState.value.data)
        assertEquals("No hay información disponible para el periodo seleccionado", vm.exportState.value.message)
        assertFalse(vm.exportState.value.isLoading)
    }

    @Test
    fun `el ownerId se envia siempre tal como lo recibe el ViewModel`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        vm.onTypeChange(FinancialTransactionType.EXPENSE)
        vm.applyFilters()
        advanceUntilIdle()
        vm.exportReport()
        awaitExport(vm)

        assertEquals(owner, service.reportCalls.single().ownerId)
        assertEquals(owner, service.exportCalls.single().ownerId)
    }
}

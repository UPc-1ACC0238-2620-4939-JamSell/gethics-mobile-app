package com.jamsell.gethics.analytics.presentation.reports

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jamsell.gethics.R
import com.jamsell.gethics.analytics.data.AnalyticsService
import com.jamsell.gethics.analytics.data.ExportedReport
import com.jamsell.gethics.analytics.data.FinanceReportSectionResponse
import com.jamsell.gethics.analytics.data.LivestockReportResponse
import com.jamsell.gethics.analytics.data.ReportSectionResponse
import com.jamsell.gethics.analytics.data.repository.AnalyticsRepository
import com.jamsell.gethics.finance.data.FinancialTransactionType
import com.jamsell.gethics.shared.ui.theme.GethicsTheme
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

/**
 * US49. Pantalla real de Compose con un servicio simulado (no toca el staging). Los textos se leen de los recursos,
 * asi la prueba vale en el idioma que tenga el dispositivo; la traduccion se verifica en ReportsStringsTest.
 * El owner es un UUID de prueba: esto NO valida identidad ni autorizacion.
 */
@RunWith(AndroidJUnit4::class)
class ReportsScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val owner = "00000000-0000-0000-0000-000000000001"

    private data class Call(val type: String?, val category: String?)

    private class FakeService : AnalyticsService {
        val reports = mutableListOf<Call>()
        val exports = mutableListOf<Call>()
        var reportHandler: (Call) -> Response<LivestockReportResponse> = { report(expense = 200.0) }
        var exportHandler: (Call) -> Response<ResponseBody> = { csv() }

        override suspend fun getReport(ownerId: String, from: String, to: String, type: String?, category: String?): Response<LivestockReportResponse> {
            val call = Call(type, category)
            reports += call
            return reportHandler(call)
        }

        override suspend fun exportReport(ownerId: String, from: String, to: String, type: String?, category: String?): Response<ResponseBody> {
            val call = Call(type, category)
            exports += call
            return exportHandler(call)
        }
    }

    private companion object {
        fun report(income: Double = 0.0, expense: Double = 0.0, limited: Boolean = false) = Response.success(
            LivestockReportResponse(
                ownerId = "o", from = "2026-03-01", to = "2026-03-31",
                finance = FinanceReportSectionResponse(income, expense, income - expense, limited, if (limited) "Datos limitados del servidor" else null),
                health = ReportSectionResponse(true, null),
                productivity = ReportSectionResponse(true, null)
            )
        )

        fun csv(): Response<ResponseBody> = Response.success(
            "a,b\r\n".toResponseBody("text/csv".toMediaType()),
            okhttp3.Headers.headersOf("Content-Disposition", "attachment; filename=\"reporte-ganado-2026-03-01-2026-03-31.csv\"")
        )

        fun <T> error(code: Int, message: String): Response<T> =
            Response.error(code, """{"message":"$message"}""".toResponseBody("application/json".toMediaType()))
    }

    private val shared = mutableListOf<ExportedReport>()

    private fun show(service: FakeService, modifier: Modifier = Modifier): ReportsViewModel {
        val viewModel = ReportsViewModel(AnalyticsRepository(service), owner)
        rule.setContent {
            GethicsTheme {
                Box(modifier) {
                    ReportsScreen(viewModel, onOpenSettings = {}, shareFile = { _, file, _ -> shared += file })
                }
            }
        }
        rule.waitForIdle()
        return viewModel
    }

    private fun text(id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    private fun chip(type: com.jamsell.gethics.finance.data.FinancialTransactionType?) =
        rule.onNodeWithTag(typeChipTag(type)).performScrollTo()

    private fun pick(label: Int) = rule.onNodeWithText(text(label)).performScrollTo().performClick()

    private fun apply() = rule.onNodeWithText(text(R.string.reports_apply_filters)).performScrollTo().performClick()

    @Test
    fun abre_con_el_reporte_sin_filtros_y_todos_seleccionado() {
        val service = FakeService()
        show(service)

        assertEquals(listOf(Call(null, null)), service.reports)
        chip(null).assertIsSelected()
        rule.onNodeWithText(text(R.string.tx_category)).assertIsDisplayed()
        rule.onNodeWithTag(ACTIVE_FILTERS_TAG).assertDoesNotExist()
    }

    @Test
    fun cambiar_los_controles_sin_aplicar_no_consulta_ni_muestra_filtros_activos() {
        val service = FakeService()
        show(service)

        chip(FinancialTransactionType.EXPENSE).performClick()
        rule.onNode(hasSetTextAction()).performScrollTo().performTextInput("Alimento")
        rule.waitForIdle()

        assertEquals(1, service.reports.size)
        rule.onNodeWithTag(ACTIVE_FILTERS_TAG).assertDoesNotExist()
    }

    @Test
    fun aplicar_tipo_y_categoria_consulta_con_ambos_y_muestra_el_indicador_de_filtros_activos() {
        val service = FakeService()
        show(service)

        chip(FinancialTransactionType.EXPENSE).performClick()
        rule.onNode(hasSetTextAction()).performScrollTo().performTextInput("Alimento")
        apply()
        rule.waitForIdle()

        assertEquals(Call("EXPENSE", "Alimento"), service.reports.last())
        val summary = text(R.string.reports_filters_active, "${text(R.string.finance_expense)} · Alimento")
        rule.onNodeWithTag(ACTIVE_FILTERS_TAG).assertExists()
        rule.onNodeWithText(summary).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun limpiar_quita_los_filtros_y_vuelve_a_consultar_sin_ellos() {
        val service = FakeService()
        show(service)
        chip(FinancialTransactionType.INCOME).performClick()
        apply()
        rule.waitForIdle()
        assertEquals(Call("INCOME", null), service.reports.last())

        pick(R.string.reports_clear_filters)
        rule.waitForIdle()

        assertEquals(Call(null, null), service.reports.last())
        rule.onNodeWithTag(ACTIVE_FILTERS_TAG).assertDoesNotExist()
        chip(null).assertIsSelected()
    }

    @Test
    fun exportar_usa_los_filtros_aplicados_y_no_lo_que_se_esta_escribiendo() {
        val service = FakeService()
        show(service)
        rule.onNode(hasSetTextAction()).performScrollTo().performTextInput("Alimento")
        chip(FinancialTransactionType.EXPENSE).performClick()
        apply()
        rule.waitForIdle()

        // Se escribe otra categoria pero no se pulsa Aplicar
        rule.onNode(hasSetTextAction()).performScrollTo().performTextInput("Veterinaria")
        pick(R.string.reports_export)
        rule.waitUntil(5_000) { shared.isNotEmpty() }

        assertEquals(listOf(Call("EXPENSE", "Alimento")), service.exports)
        assertEquals("reporte-ganado-2026-03-01-2026-03-31.csv", shared.single().fileName)
        rule.onNodeWithText(text(R.string.reports_exported, shared.single().fileName)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun muestra_el_reporte_filtrado_con_sus_totales() {
        val service = FakeService()
        service.reportHandler = { report(income = 0.0, expense = 200.0) }
        show(service)

        rule.onNodeWithText(text(R.string.reports_finance)).assertIsDisplayed()
        rule.onNodeWithText("-200", substring = true).assertExists()
    }

    @Test
    fun sin_resultados_con_filtros_muestra_el_aviso_del_servidor_y_el_de_filtros() {
        val service = FakeService()
        service.reportHandler = { call -> if (call.category == null) report(expense = 200.0) else report(limited = true) }
        show(service)
        rule.onNode(hasSetTextAction()).performScrollTo().performTextInput("Nada")
        apply()
        rule.waitForIdle()

        rule.onNodeWithText("Datos limitados del servidor").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(text(R.string.reports_no_results_filtered)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun un_404_de_exportacion_sin_datos_se_informa_y_no_comparte_nada() {
        val service = FakeService()
        service.exportHandler = { error(404, "No hay datos para exportar con esos filtros") }
        show(service)

        pick(R.string.reports_export)
        rule.waitForIdle()

        rule.onNodeWithText("No hay datos para exportar con esos filtros").performScrollTo().assertIsDisplayed()
        assertEquals(emptyList<ExportedReport>(), shared)
    }

    @Test
    fun un_400_del_reporte_se_muestra_como_mensaje_sin_romper_la_pantalla() {
        val service = FakeService()
        service.reportHandler = { error(400, "Parametro invalido: type") }
        show(service)

        rule.onNodeWithText("Parametro invalido: type").performScrollTo().assertIsDisplayed()
        apply() // se puede reintentar
        rule.waitForIdle()
        assertEquals(2, service.reports.size)
    }

    @Test
    fun en_pantalla_pequena_todos_los_controles_se_alcanzan_con_scroll() {
        show(FakeService(), Modifier.size(width = 320.dp, height = 480.dp))

        chip(null).assertIsDisplayed()
        rule.onNode(hasSetTextAction()).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(text(R.string.reports_apply_filters)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(text(R.string.reports_clear_filters)).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(text(R.string.reports_export)).performScrollTo().assertIsDisplayed()
    }

}

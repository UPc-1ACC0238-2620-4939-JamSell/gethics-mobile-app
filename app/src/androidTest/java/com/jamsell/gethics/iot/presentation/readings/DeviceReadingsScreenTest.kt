package com.jamsell.gethics.iot.presentation.readings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jamsell.gethics.R
import com.jamsell.gethics.iot.data.DeviceResponse
import com.jamsell.gethics.iot.data.IotService
import com.jamsell.gethics.iot.data.LinkDeviceRequest
import com.jamsell.gethics.iot.data.ReadingsPageResponse
import com.jamsell.gethics.iot.data.SensorReadingResponse
import com.jamsell.gethics.iot.data.repository.IotRepository
import com.jamsell.gethics.shared.ui.theme.GethicsTheme
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

/**
 * US48. Pantalla real de Compose con un servicio simulado (no toca el staging).
 * Los textos se leen de los recursos, asi la prueba vale en el idioma que tenga el dispositivo.
 */
@RunWith(AndroidJUnit4::class)
class DeviceReadingsScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val deviceId = "aaaaaaaa-0000-0000-0000-00000000000a"

    private class FakeService(var handler: (page: Int, from: String?, to: String?) -> Response<ReadingsPageResponse>) : IotService {
        val calls = mutableListOf<Pair<String?, String?>>()
        val pages = mutableListOf<Int>()

        override suspend fun getReadings(deviceId: String, page: Int, size: Int, from: String?, to: String?): Response<ReadingsPageResponse> {
            calls += from to to
            pages += page
            return handler(page, from, to)
        }

        override suspend fun linkDevice(request: LinkDeviceRequest): Response<DeviceResponse> = throw UnsupportedOperationException()
        override suspend fun getDevices(ownerId: String): Response<List<DeviceResponse>> = throw UnsupportedOperationException()
    }

    private fun readings(vararg metrics: String, totalPages: Int = 1, page: Int = 0) = Response.success(
        ReadingsPageResponse(
            content = metrics.mapIndexed { i, m -> SensorReadingResponse("id-$page-$m-$i", m, 38.2, "2026-11-27T08:00:00") },
            page = page,
            size = 20,
            totalElements = (metrics.size * totalPages).toLong(),
            totalPages = totalPages
        )
    )

    private fun empty() = Response.success(ReadingsPageResponse(emptyList(), 0, 20, 0, 0))

    private fun show(service: FakeService, deviceId: String = this.deviceId): DeviceReadingsViewModel {
        val viewModel = DeviceReadingsViewModel(IotRepository(service), deviceId)
        rule.setContent { GethicsTheme { DeviceReadingsScreen(viewModel, deviceCode = "COLLAR-7", onBack = {}) } }
        rule.waitForIdle()
        return viewModel
    }

    private fun text(id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    @Test
    fun muestra_las_lecturas_con_metrica_valor_y_fecha_sin_cambiar_la_zona() {
        show(FakeService { _, _, _ -> readings("temperature") })

        rule.onNodeWithText("temperature").assertIsDisplayed()
        rule.onNodeWithText("38.20").assertIsDisplayed()
        rule.onNodeWithText("27/11/2026 08:00").assertIsDisplayed() // recordedAt tal cual, sin conversion de zona
        rule.onNodeWithText(text(R.string.iot_readings_device, "COLLAR-7")).assertIsDisplayed()
    }

    @Test
    fun sin_lecturas_muestra_el_estado_vacio() {
        show(FakeService { _, _, _ -> empty() })

        rule.onNodeWithText(text(R.string.iot_readings_empty)).assertIsDisplayed()
    }

    @Test
    fun aplicar_y_limpiar_filtros_actualiza_el_indicador_y_la_consulta() {
        val service = FakeService { _, from, _ -> if (from != null) empty() else readings("temperature") }
        val viewModel = show(service)

        rule.runOnIdle {
            viewModel.onFromChange(LocalDate.of(2026, 11, 1))
            viewModel.onToChange(LocalDate.of(2026, 11, 27))
        }
        rule.onNodeWithTag(TAG_APPLY).performClick()
        rule.waitForIdle()

        assertEquals("2026-11-01" to "2026-11-27", service.calls.last())
        rule.onNodeWithText(text(R.string.iot_readings_empty_filtered)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.iot_filter_active, "").trim(), substring = true).assertIsDisplayed()

        rule.onNodeWithTag(TAG_CLEAR).performClick()
        rule.waitForIdle()

        assertEquals(null to null, service.calls.last())
        rule.onNodeWithText("temperature").assertIsDisplayed()
        rule.onAllNodesWithText(text(R.string.iot_filter_not_set)).assertCountEquals(2) // fecha inicial y final vacias
    }

    @Test
    fun rango_invertido_muestra_el_aviso_y_no_consulta() {
        val service = FakeService { _, _, _ -> readings("temperature") }
        val viewModel = show(service)

        rule.runOnIdle {
            viewModel.onFromChange(LocalDate.of(2026, 11, 27))
            viewModel.onToChange(LocalDate.of(2026, 11, 1))
        }
        rule.onNodeWithTag(TAG_APPLY).performClick()
        rule.waitForIdle()

        rule.onNodeWithText(text(R.string.iot_filter_invalid_range)).assertIsDisplayed()
        assertEquals(1, service.calls.size)
    }

    @Test
    fun error_de_conexion_se_muestra_traducido_y_reintentar_recupera() {
        var fail = true
        val service = FakeService { _, _, _ -> if (fail) throw IOException("sin red") else readings("humidity") }
        show(service)

        rule.onNodeWithText(text(R.string.iot_readings_error_connection)).assertIsDisplayed()

        fail = false
        rule.onNodeWithText(text(R.string.iot_readings_retry)).performClick()
        rule.waitForIdle()

        rule.onNodeWithText("humidity").assertIsDisplayed()
    }

    @Test
    fun error_del_backend_muestra_su_mensaje() {
        val body = """{"message":"Dispositivo no encontrado"}""".toResponseBody("application/json".toMediaType())
        show(FakeService { _, _, _ -> Response.error(404, body) })

        rule.onNodeWithText("Dispositivo no encontrado").assertIsDisplayed()
    }

    // Scroll infinito (US37): al llegar al final se pide la siguiente pagina, una sola vez, y no mas alla de la ultima.
    @Test
    fun al_llegar_al_final_carga_la_siguiente_pagina_una_sola_vez_y_muestra_el_progreso() {
        val service = FakeService { page, _, _ ->
            if (page == 0) readings("temperature", totalPages = 2) else readings("humidity", totalPages = 2, page = 1)
        }
        show(service)

        rule.onNodeWithText("humidity").assertIsDisplayed()
        rule.onNodeWithText(text(R.string.iot_readings_progress, 2, 2L)).assertIsDisplayed()
        assertEquals(listOf(0, 1), service.pages) // ni duplicadas ni una pagina 2 inexistente
    }

    @Test
    fun una_sola_pagina_no_pide_mas() {
        val service = FakeService { _, _, _ -> readings("temperature") }
        show(service)

        rule.onNodeWithText(text(R.string.iot_readings_progress, 1, 1L)).assertIsDisplayed()
        assertEquals(listOf(0), service.pages)
    }

    @Test
    fun lista_larga_no_pide_la_siguiente_hasta_llegar_al_final() {
        val metrics = Array(40) { "metrica$it" }
        val service = FakeService { page, _, _ ->
            if (page == 0) readings(*metrics, totalPages = 2) else readings("final", totalPages = 2, page = 1)
        }
        show(service)

        assertEquals(listOf(0), service.pages) // el final de la lista aun no esta en pantalla

        rule.onNode(hasScrollAction()).performScrollToIndex(41) // filtros + 40 lecturas + pie
        rule.waitForIdle()

        assertEquals(listOf(0, 1), service.pages)
    }

    @Test
    fun fallo_al_paginar_muestra_reintentar_y_no_repite_solo() {
        var fail = true
        val body = """{"message":"Servicio no disponible"}""".toResponseBody("application/json".toMediaType())
        val service = FakeService { page, _, _ ->
            when {
                page == 0 -> readings("temperature", totalPages = 2)
                fail -> Response.error(503, body)
                else -> readings("humidity", totalPages = 2, page = 1)
            }
        }
        show(service)

        rule.onNodeWithText("Servicio no disponible").assertIsDisplayed()
        rule.onNodeWithText("temperature").assertIsDisplayed() // lo ya cargado se conserva
        assertEquals(listOf(0, 1), service.pages) // sin bucle de reintentos

        fail = false
        rule.onNodeWithText(text(R.string.iot_readings_retry)).performClick()
        rule.waitForIdle()

        rule.onNodeWithText("humidity").assertIsDisplayed()
        assertEquals(listOf(0, 1, 1), service.pages)
    }

    @Test
    fun aplicar_filtros_reinicia_la_paginacion_y_las_paginas_siguientes_usan_el_filtro() {
        val service = FakeService { page, from, _ ->
            val tag = from ?: "sin"
            if (page == 0) readings("p0-$tag", totalPages = 2) else readings("p1-$tag", totalPages = 2, page = 1)
        }
        val viewModel = show(service)
        assertEquals(listOf(0, 1), service.pages)

        rule.runOnIdle { viewModel.onFromChange(LocalDate.of(2026, 11, 1)) }
        rule.onNodeWithTag(TAG_APPLY).performClick()
        rule.waitForIdle()

        assertEquals(listOf(0, 1, 0, 1), service.pages)
        assertEquals("2026-11-01", service.calls.last().first) // la pagina 1 viaja con el filtro aplicado
        rule.onNodeWithText("p1-2026-11-01").assertIsDisplayed()
        rule.onNodeWithText("p1-sin").assertDoesNotExist() // sin mezclar con lo anterior
    }

    @Test
    fun el_selector_de_fecha_abre_el_calendario_y_cancelar_lo_cierra() {
        show(FakeService { _, _, _ -> readings("temperature") })

        rule.onNodeWithTag(TAG_FROM).performClick()
        rule.onNodeWithText(text(R.string.common_accept)).assertIsDisplayed()

        rule.onNodeWithText(text(R.string.common_cancel)).performClick()
        rule.onNodeWithTag(TAG_FROM).assertIsDisplayed()
    }

    @Test
    fun sin_dispositivo_valido_avisa_y_no_consulta() {
        val service = FakeService { _, _, _ -> readings("temperature") }
        show(service, deviceId = "")

        rule.onNodeWithText(text(R.string.iot_readings_device_unavailable)).assertIsDisplayed()
        assertEquals(0, service.calls.size)
    }
}

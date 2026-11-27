package com.jamsell.gethics.iot.presentation.readings

import com.jamsell.gethics.iot.data.DeviceResponse
import com.jamsell.gethics.iot.data.IotService
import com.jamsell.gethics.iot.data.LinkDeviceRequest
import com.jamsell.gethics.iot.data.ReadingsPageResponse
import com.jamsell.gethics.iot.data.SensorReadingResponse
import com.jamsell.gethics.iot.data.repository.IotRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceReadingsViewModelTest {

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val deviceA = "aaaaaaaa-0000-0000-0000-00000000000a"
    private val deviceB = "bbbbbbbb-0000-0000-0000-00000000000b"
    private val nov1 = LocalDate.of(2026, 11, 1)
    private val nov27 = LocalDate.of(2026, 11, 27)

    private data class Call(val id: String, val page: Int, val size: Int, val from: String?, val to: String?)

    /** Registra cada consulta y responde con handler (por defecto, una pagina de totalPages con 2 lecturas por pagina). */
    private class FakeService : IotService {
        val calls = mutableListOf<Call>()
        var totalPages = 1
        var handler: suspend (Call) -> Response<ReadingsPageResponse> = { call -> page(call.page, totalPages, "${call.id}-${call.from}-${call.to}") }

        override suspend fun getReadings(deviceId: String, page: Int, size: Int, from: String?, to: String?): Response<ReadingsPageResponse> {
            val call = Call(deviceId, page, size, from, to)
            calls += call
            return handler(call)
        }

        override suspend fun linkDevice(request: LinkDeviceRequest): Response<DeviceResponse> = throw UnsupportedOperationException()
        override suspend fun getDevices(ownerId: String): Response<List<DeviceResponse>> = throw UnsupportedOperationException()
    }

    private companion object {
        fun page(page: Int, totalPages: Int, tag: String): Response<ReadingsPageResponse> = Response.success(
            ReadingsPageResponse(
                content = (0..1).map { SensorReadingResponse("$tag-p$page-$it", "temperature", 38.0 + it, "2026-11-27T08:0$it:00") },
                page = page,
                size = READINGS_PAGE_SIZE,
                totalElements = totalPages * 2L,
                totalPages = totalPages
            )
        )

        fun error(code: Int, message: String): Response<ReadingsPageResponse> =
            Response.error(code, """{"message":"$message"}""".toResponseBody("application/json".toMediaType()))
    }

    private fun viewModel(service: FakeService, deviceId: String = deviceA) =
        DeviceReadingsViewModel(IotRepository(service), deviceId)

    @Test
    fun `carga inicial consulta la pagina 0 sin filtros`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        advanceUntilIdle()

        assertEquals(listOf(Call(deviceA, 0, READINGS_PAGE_SIZE, null, null)), service.calls)
        assertEquals(2, vm.readings.size)
        assertTrue(vm.loadedOnce)
        assertFalse(vm.isLoading)
        assertFalse(vm.hasActiveFilters)
    }

    @Test
    fun `aplicar filtros reinicia en la pagina 0 y reemplaza las lecturas`() = runTest {
        val service = FakeService().apply { totalPages = 3 }
        val vm = viewModel(service)
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle() // ya estamos en la pagina 1

        vm.onFromChange(nov1)
        vm.onToChange(nov27)
        vm.applyFilters()
        advanceUntilIdle()

        assertEquals(Call(deviceA, 0, READINGS_PAGE_SIZE, "2026-11-01", "2026-11-27"), service.calls.last())
        assertEquals(2, vm.readings.size) // solo la pagina 0 del nuevo filtro, sin mezclar
        assertTrue(vm.readings.all { it.id.contains("2026-11-01-2026-11-27") })
        assertTrue(vm.hasActiveFilters)
    }

    @Test
    fun `editar las fechas sin aplicar no consulta`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        advanceUntilIdle()

        vm.onFromChange(nov1)
        vm.onToChange(nov27)
        advanceUntilIdle()

        assertEquals(1, service.calls.size)
        assertFalse(vm.hasActiveFilters)
    }

    @Test
    fun `rango abierto solo con from o solo con to`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        advanceUntilIdle()

        vm.onFromChange(nov1)
        vm.applyFilters()
        advanceUntilIdle()
        assertEquals("2026-11-01", service.calls.last().from)
        assertEquals(null, service.calls.last().to)

        vm.onFromChange(null)
        vm.onToChange(nov27)
        vm.applyFilters()
        advanceUntilIdle()
        assertEquals(null, service.calls.last().from)
        assertEquals("2026-11-27", service.calls.last().to)
    }

    @Test
    fun `un mismo dia es un rango valido`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        advanceUntilIdle()

        vm.onFromChange(nov27)
        vm.onToChange(nov27)
        vm.applyFilters()
        advanceUntilIdle()

        assertFalse(vm.rangeInvalid)
        assertEquals("2026-11-27", service.calls.last().from)
        assertEquals("2026-11-27", service.calls.last().to)
    }

    @Test
    fun `rango invertido no consulta y marca el error hasta que se edita una fecha`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        advanceUntilIdle()

        vm.onFromChange(nov27)
        vm.onToChange(nov1)
        vm.applyFilters()
        advanceUntilIdle()

        assertTrue(vm.rangeInvalid)
        assertEquals(1, service.calls.size) // solo la carga inicial
        assertFalse(vm.hasActiveFilters)
        assertEquals(2, vm.readings.size) // la lista anterior se conserva

        vm.onFromChange(nov1)
        assertFalse(vm.rangeInvalid)
    }

    @Test
    fun `limpiar filtros vuelve a consultar sin fechas desde la pagina 0`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        advanceUntilIdle()
        vm.onFromChange(nov1)
        vm.onToChange(nov27)
        vm.applyFilters()
        advanceUntilIdle()

        vm.clearFilters()
        advanceUntilIdle()

        assertEquals(Call(deviceA, 0, READINGS_PAGE_SIZE, null, null), service.calls.last())
        assertEquals(null, vm.from)
        assertEquals(null, vm.to)
        assertFalse(vm.hasActiveFilters)
        assertFalse(vm.rangeInvalid)
    }

    @Test
    fun `paginacion acumula paginas y se detiene en la ultima`() = runTest {
        val service = FakeService().apply { totalPages = 2 }
        val vm = viewModel(service)
        advanceUntilIdle()
        assertTrue(vm.hasMore)
        assertEquals(4L, vm.totalElements)
        assertEquals(2, vm.totalPages)

        vm.loadMore()
        advanceUntilIdle()

        assertEquals(1, service.calls.last().page)
        assertEquals(4, vm.readings.size)
        assertFalse(vm.hasMore)

        vm.loadMore() // ya no hay mas: no consulta
        advanceUntilIdle()
        assertEquals(2, service.calls.size)
    }

    @Test
    fun `las paginas siguientes conservan el filtro aplicado aunque el borrador cambie`() = runTest {
        val service = FakeService().apply { totalPages = 3 }
        val vm = viewModel(service)
        advanceUntilIdle()
        vm.onFromChange(nov1)
        vm.onToChange(nov27)
        vm.applyFilters()
        advanceUntilIdle()

        vm.onFromChange(LocalDate.of(2026, 10, 1)) // borrador sin aplicar
        vm.loadMore()
        advanceUntilIdle()

        assertEquals(Call(deviceA, 1, READINGS_PAGE_SIZE, "2026-11-01", "2026-11-27"), service.calls.last())
    }

    @Test
    fun `cambiar de dispositivo descarta lecturas y filtros y consulta el nuevo`() = runTest {
        val service = FakeService()
        val vm = viewModel(service)
        advanceUntilIdle()
        vm.onFromChange(nov1)
        vm.applyFilters()
        advanceUntilIdle()

        vm.selectDevice(deviceB)
        advanceUntilIdle()

        assertEquals(Call(deviceB, 0, READINGS_PAGE_SIZE, null, null), service.calls.last())
        assertEquals(deviceB, vm.deviceId)
        assertTrue(vm.readings.all { it.id.startsWith(deviceB) })
        assertEquals(null, vm.from)
        assertFalse(vm.hasActiveFilters)
    }

    @Test
    fun `una respuesta tardia de filtros anteriores se descarta`() = runTest {
        val service = FakeService()
        val first = CompletableDeferred<Response<ReadingsPageResponse>>()
        var firstPending = true
        service.handler = { call ->
            // la 1a consulta con filtro queda en vuelo (sin cancelacion, para probar el descarte y no la cancelacion)
            if (call.from == "2026-11-01" && firstPending) {
                firstPending = false
                withContext(NonCancellable) { first.await() }
            } else {
                page(call.page, 1, "${call.from}-${call.to}")
            }
        }
        val vm = viewModel(service)
        advanceUntilIdle()

        vm.onFromChange(nov1)
        vm.applyFilters() // consulta A: queda esperando
        advanceUntilIdle()
        vm.onFromChange(nov27)
        vm.applyFilters() // consulta B: responde de inmediato
        advanceUntilIdle()
        first.complete(page(0, 1, "vieja")) // A responde tarde
        advanceUntilIdle()

        assertTrue(vm.readings.all { it.id.startsWith("2026-11-27") })
        assertFalse(vm.readings.any { it.id.startsWith("vieja") })
        assertFalse(vm.isLoading)
    }

    @Test
    fun `una respuesta tardia del dispositivo anterior se descarta`() = runTest {
        val service = FakeService()
        val late = CompletableDeferred<Response<ReadingsPageResponse>>()
        service.handler = { call ->
            if (call.id == deviceA && service.calls.size == 1) withContext(NonCancellable) { late.await() } else page(call.page, 1, call.id)
        }
        val vm = viewModel(service) // consulta del dispositivo A en vuelo
        advanceUntilIdle()

        vm.selectDevice(deviceB)
        advanceUntilIdle()
        late.complete(page(0, 1, deviceA))
        advanceUntilIdle()

        assertTrue(vm.readings.isNotEmpty())
        assertTrue(vm.readings.all { it.id.startsWith(deviceB) })
    }

    @Test
    fun `no duplica solicitudes mientras hay una en curso`() = runTest {
        val service = FakeService().apply { totalPages = 3 }
        val gate = CompletableDeferred<Unit>()
        service.handler = { call ->
            if (call.page == 1) gate.await()
            page(call.page, 3, "x")
        }
        val vm = viewModel(service)
        advanceUntilIdle()

        vm.loadMore()
        vm.loadMore()
        vm.retry()
        advanceUntilIdle()
        assertEquals(2, service.calls.size) // carga inicial + una sola pagina 1

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(4, vm.readings.size)
    }

    @Test
    fun `aplicar el mismo filtro mientras carga no repite la consulta`() = runTest {
        val service = FakeService()
        val gate = CompletableDeferred<Unit>()
        service.handler = { call ->
            if (call.from != null) gate.await()
            page(call.page, 1, "x")
        }
        val vm = viewModel(service)
        advanceUntilIdle()

        vm.onFromChange(nov1)
        vm.applyFilters()
        vm.applyFilters()
        advanceUntilIdle()
        assertEquals(2, service.calls.size)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `sin dispositivo valido no se consulta nada`() = runTest {
        for (id in listOf("", "   ", "no-es-un-uuid")) {
            val service = FakeService()
            val vm = viewModel(service, deviceId = id)
            advanceUntilIdle()

            vm.onFromChange(nov1)
            vm.applyFilters()
            vm.clearFilters()
            vm.loadMore()
            vm.retry()
            advanceUntilIdle()

            assertFalse(vm.hasValidDevice)
            assertTrue("id=$id", service.calls.isEmpty())
            assertFalse(vm.isLoading)
        }
    }

    @Test
    fun `un dispositivo valido seleccionado despues activa la consulta`() = runTest {
        val service = FakeService()
        val vm = viewModel(service, deviceId = "")
        advanceUntilIdle()
        assertTrue(service.calls.isEmpty())

        vm.selectDevice(deviceA)
        advanceUntilIdle()

        assertEquals(1, service.calls.size)
        assertTrue(vm.hasValidDevice)
    }

    @Test
    fun `error 404 muestra el mensaje del backend y reintentar vuelve a pedir la misma pagina`() = runTest {
        val service = FakeService()
        var fail = true
        service.handler = { call -> if (fail) error(404, "Dispositivo no encontrado") else page(call.page, 1, "ok") }
        val vm = viewModel(service)
        advanceUntilIdle()

        assertEquals("Dispositivo no encontrado", vm.message)
        assertTrue(vm.hasError)
        assertFalse(vm.connectionError)
        assertFalse(vm.loadedOnce)

        fail = false
        vm.retry()
        advanceUntilIdle()

        assertEquals(listOf(0, 0), service.calls.map { it.page })
        assertFalse(vm.hasError)
        assertEquals(2, vm.readings.size)
    }

    @Test
    fun `error de conexion se marca aparte para mostrarlo en el idioma actual`() = runTest {
        val service = FakeService()
        service.handler = { throw IOException("sin red") }
        val vm = viewModel(service)
        advanceUntilIdle()

        assertTrue(vm.connectionError)
        assertEquals("", vm.message)
        assertTrue(vm.hasError)
    }

    @Test
    fun `fallo al paginar conserva lo cargado y reintenta la pagina que fallo`() = runTest {
        val service = FakeService().apply { totalPages = 2 }
        var failPage1 = true
        service.handler = { call ->
            if (call.page == 1 && failPage1) error(400, "size invalido") else page(call.page, 2, "x")
        }
        val vm = viewModel(service)
        advanceUntilIdle()

        vm.loadMore()
        advanceUntilIdle()
        assertEquals(2, vm.readings.size)
        assertEquals("size invalido", vm.message)

        failPage1 = false
        vm.retry()
        advanceUntilIdle()
        assertEquals(1, service.calls.last().page)
        assertEquals(4, vm.readings.size)
        assertFalse(vm.hasError)
    }

    @Test
    fun `sin lecturas en el rango es un estado vacio y no un error`() = runTest {
        val service = FakeService()
        service.handler = {
            Response.success(ReadingsPageResponse(emptyList(), 0, READINGS_PAGE_SIZE, 0, 0))
        }
        val vm = viewModel(service)
        advanceUntilIdle()
        vm.onToChange(nov27)
        vm.applyFilters()
        advanceUntilIdle()

        assertTrue(vm.readings.isEmpty())
        assertTrue(vm.loadedOnce)
        assertFalse(vm.hasError)
        assertFalse(vm.hasMore)
        assertTrue(vm.hasActiveFilters)
    }

    @Test
    fun `el scroll infinito no reintenta solo tras un fallo y no pide mas alla de la ultima pagina`() = runTest {
        val service = FakeService().apply { totalPages = 2 }
        service.handler = { call -> if (call.page == 1) error(503, "caido") else page(call.page, 2, "x") }
        val vm = viewModel(service)
        advanceUntilIdle()

        repeat(3) { vm.loadMore() } // lo que dispararia el final de la lista, repetido por recomposicion
        advanceUntilIdle()
        assertEquals(listOf(0, 1), service.calls.map { it.page }) // una sola peticion de la pagina 1
        assertTrue(vm.hasError)

        vm.loadMore() // con el error activo no hay reintento automatico
        advanceUntilIdle()
        assertEquals(2, service.calls.size)

        service.handler = { call -> page(call.page, 2, "x") }
        vm.retry()
        advanceUntilIdle()
        assertEquals(listOf(0, 1, 1), service.calls.map { it.page })
        assertFalse(vm.hasMore)

        repeat(3) { vm.loadMore() } // ultima pagina: nada mas que pedir
        advanceUntilIdle()
        assertEquals(3, service.calls.size)
    }
}

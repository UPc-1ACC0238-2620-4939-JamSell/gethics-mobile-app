package com.jamsell.gethics.livestock.presentation.farm_edit

import com.jamsell.gethics.livestock.data.FarmListResponse
import com.jamsell.gethics.livestock.data.FarmResponse
import com.jamsell.gethics.livestock.data.FarmService
import com.jamsell.gethics.livestock.data.UpdateFarmRequest
import com.jamsell.gethics.livestock.data.repository.FarmRepository
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

/** US-32: validacion del formulario y guardado de la granja. */
@OptIn(ExperimentalCoroutinesApi::class)
class FarmEditViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeFarmService(val update: () -> Response<FarmResponse>) : FarmService {
        val updates = mutableListOf<UpdateFarmRequest>()

        override suspend fun getFarms(ownerId: String) =
            Response.success(FarmListResponse(listOf(FarmResponse("f1", ownerId, "La Esperanza", "Cusco", 12.5, "ACTIVE")), null))

        override suspend fun updateFarm(farmId: String, request: UpdateFarmRequest): Response<FarmResponse> {
            updates += request
            return update()
        }
    }

    private fun ok() = Response.success(FarmResponse("f1", "o1", "N", "U", null, "ACTIVE"))

    private fun viewModel(service: FakeFarmService) = FarmEditViewModel(FarmRepository(service), "f1", "o1")

    // ---------------- validacion pura ----------------

    @Test
    fun `parseSize acepta numeros con hasta 2 decimales y coma, y rechaza el resto`() {
        assertEquals(12.5, parseSize("12.5")!!, 0.0)
        assertEquals(12.5, parseSize("12,5")!!, 0.0)
        assertEquals(0.01, parseSize("0.01")!!, 0.0)
        assertNull(parseSize("0"))
        assertNull(parseSize("-3"))
        assertNull(parseSize("1.234"))
        assertNull(parseSize("abc"))
        assertNull(parseSize("12345678"))
    }

    @Test
    fun `validateFarm exige nombre y ubicacion, respeta los maximos y permite tamano vacio`() {
        assertFalse(validateFarm("La Esperanza", "Cusco", "").hasErrors)
        assertEquals(FARM_NAME_REQUIRED_ERROR, validateFarm("  ", "Cusco", "").name)
        assertEquals(FARM_NAME_TOO_LONG_ERROR, validateFarm("x".repeat(101), "Cusco", "").name)
        assertFalse(validateFarm("x".repeat(100), "Cusco", "").hasErrors)
        assertEquals(FARM_LOCATION_REQUIRED_ERROR, validateFarm("N", "", "").location)
        assertEquals(FARM_LOCATION_TOO_LONG_ERROR, validateFarm("N", "u".repeat(201), "").location)
        assertEquals(FARM_SIZE_INVALID_ERROR, validateFarm("N", "U", "0").size)
    }

    // ---------------- ViewModel ----------------

    @Test
    fun `al abrir carga la granja para precargar el formulario`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeFarmService { ok() })
        advanceUntilIdle()

        assertEquals("La Esperanza", viewModel.farm.value.data?.name)
    }

    @Test
    fun `guardar con datos invalidos no llama al backend y marca los errores`() = runTest(dispatcher) {
        val service = FakeFarmService { ok() }
        val viewModel = viewModel(service)
        advanceUntilIdle()

        viewModel.save("", "", "x")
        advanceUntilIdle()

        assertTrue(viewModel.errors.value.hasErrors)
        assertTrue(service.updates.isEmpty())
        assertNull(viewModel.saveState.value.data)
    }

    @Test
    fun `escenario 1 guardar datos validos recorta espacios y avisa a la pantalla`() = runTest(dispatcher) {
        val service = FakeFarmService { ok() }
        val viewModel = viewModel(service)
        advanceUntilIdle()

        viewModel.save("  Nueva  ", " Cusco ", "20,5")
        advanceUntilIdle()

        assertEquals(UpdateFarmRequest("Nueva", "Cusco", 20.5), service.updates.single())
        assertEquals(true, viewModel.saveState.value.data)
        assertFalse(viewModel.errors.value.hasErrors)
    }

    @Test
    fun `tamano vacio se envia como null`() = runTest(dispatcher) {
        val service = FakeFarmService { ok() }
        val viewModel = viewModel(service)
        advanceUntilIdle()

        viewModel.save("Nueva", "Cusco", "")
        advanceUntilIdle()

        assertNull(service.updates.single().sizeHectares)
    }

    @Test
    fun `409 nombre repetido muestra el message del backend y no cierra la pantalla`() = runTest(dispatcher) {
        val service = FakeFarmService {
            Response.error(409, """{"message":"Ya tienes otra granja con ese nombre."}""".toResponseBody("application/json".toMediaType()))
        }
        val viewModel = viewModel(service)
        advanceUntilIdle()

        viewModel.save("Dup", "Cusco", "")
        advanceUntilIdle()

        assertEquals("Ya tienes otra granja con ese nombre.", viewModel.saveState.value.message)
        assertNull(viewModel.saveState.value.data)
    }
}

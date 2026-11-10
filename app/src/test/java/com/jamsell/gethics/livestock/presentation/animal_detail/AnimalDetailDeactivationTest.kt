package com.jamsell.gethics.livestock.presentation.animal_detail

import com.jamsell.gethics.livestock.data.AnimalListResponse
import com.jamsell.gethics.livestock.data.AnimalResponse
import com.jamsell.gethics.livestock.data.LivestockService
import com.jamsell.gethics.livestock.data.RegisterAnimalRequest
import com.jamsell.gethics.livestock.data.UpdateAnimalRequest
import com.jamsell.gethics.livestock.data.local.AnimalDao
import com.jamsell.gethics.livestock.data.local.AnimalEntity
import com.jamsell.gethics.livestock.data.repository.LivestockRepository
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
import java.io.IOException

/** US08 en la ficha del animal: Escenario 1 (baja confirmada) y Escenario 2 (baja cancelada). */
@OptIn(ExperimentalCoroutinesApi::class)
class AnimalDetailDeactivationTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeService(val deactivate: () -> Response<Unit>) : LivestockService {
        var deactivateCalls = 0

        override suspend fun deactivateAnimal(animalId: String): Response<Unit> {
            deactivateCalls++
            return deactivate()
        }

        override suspend fun getAnimal(animalId: String): Response<AnimalResponse> = error("no usado")
        override suspend fun sellAnimal(animalId: String, request: com.jamsell.gethics.livestock.data.SellAnimalRequest): Response<com.jamsell.gethics.livestock.data.AnimalSaleResponse> = error("no usado")
        override suspend fun getAnimalByQr(code: String): Response<AnimalResponse> = error("no usado")
        override suspend fun updateAnimal(animalId: String, request: UpdateAnimalRequest): Response<AnimalResponse> = error("no usado")
        override suspend fun registerAnimal(request: RegisterAnimalRequest): Response<AnimalResponse> = error("no usado")
        override suspend fun getAnimals(search: String?, status: String?): Response<AnimalListResponse> = error("no usado")
    }

    private class FakeDao : AnimalDao {
        val saved = mutableListOf(
            AnimalEntity("a1", "Luna", "MX-1", "Holstein", null, "2022-05-12", 650.0, "GTH-1", "ACTIVE", "FEMALE", "farm-1")
        )
        override suspend fun insert(animal: AnimalEntity) { saved.removeAll { it.id == animal.id }; saved += animal }
        override suspend fun insertAll(animals: List<AnimalEntity>) { animals.forEach { insert(it) } }
        override suspend fun fetchAll() = saved.toList()
        override suspend fun fetchById(id: String) = saved.firstOrNull { it.id == id }
        override suspend fun search(pattern: String, status: String) = emptyList<AnimalEntity>()
        override suspend fun deleteByStatus(status: String) { saved.removeAll { it.status == status } }
    }

    private fun failure(code: Int, body: String): Response<Unit> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    private fun viewModel(service: FakeService, dao: FakeDao = FakeDao()) =
        AnimalDetailViewModel(LivestockRepository(service, dao), "a1")

    @Test
    fun `al inicio no hay dialogo ni baja ni error`() {
        val viewModel = viewModel(FakeService { Response.success(Unit) })

        assertFalse(viewModel.confirmingDeactivation.value)
        assertFalse(viewModel.deactivating.value)
        assertFalse(viewModel.deactivated.value)
        assertNull(viewModel.deactivationError.value)
    }

    @Test
    fun `pedir la baja solo abre la confirmacion y no llama al backend`() = runTest(dispatcher) {
        val service = FakeService { Response.success(Unit) }
        val viewModel = viewModel(service)

        viewModel.requestDeactivation()
        advanceUntilIdle()

        assertTrue(viewModel.confirmingDeactivation.value)
        assertEquals(0, service.deactivateCalls)
        assertFalse(viewModel.deactivated.value)
    }

    @Test
    fun `escenario 2 cancelar cierra el dialogo y el animal queda sin cambios`() = runTest(dispatcher) {
        val service = FakeService { Response.success(Unit) }
        val dao = FakeDao()
        val viewModel = viewModel(service, dao)
        viewModel.requestDeactivation()

        viewModel.cancelDeactivation()
        advanceUntilIdle()

        assertFalse(viewModel.confirmingDeactivation.value)
        assertFalse(viewModel.deactivated.value)
        assertEquals(0, service.deactivateCalls)
        assertEquals("ACTIVE", dao.saved.single().status)
    }

    @Test
    fun `escenario 1 confirmar da de baja el animal y avisa a la pantalla`() = runTest(dispatcher) {
        val service = FakeService { Response.success(Unit) }
        val dao = FakeDao()
        val viewModel = viewModel(service, dao)
        viewModel.requestDeactivation()

        viewModel.confirmDeactivation()
        advanceUntilIdle()

        assertTrue(viewModel.deactivated.value)
        assertFalse(viewModel.confirmingDeactivation.value)
        assertFalse(viewModel.deactivating.value)
        assertNull(viewModel.deactivationError.value)
        assertEquals(1, service.deactivateCalls)
        assertEquals("INACTIVE", dao.saved.single().status)
    }

    @Test
    fun `un doble toque en confirmar no da de baja dos veces`() = runTest(dispatcher) {
        val service = FakeService { Response.success(Unit) }
        val viewModel = viewModel(service)

        viewModel.confirmDeactivation()
        viewModel.confirmDeactivation()
        advanceUntilIdle()

        assertEquals(1, service.deactivateCalls)
    }

    @Test
    fun `si el backend responde 409 muestra su mensaje y no marca la baja`() = runTest(dispatcher) {
        val service = FakeService { failure(409, """{"message":"El animal ya esta dado de baja."}""") }
        val viewModel = viewModel(service)

        viewModel.confirmDeactivation()
        advanceUntilIdle()

        assertFalse(viewModel.deactivated.value)
        assertFalse(viewModel.deactivating.value)
        assertEquals("El animal ya esta dado de baja.", viewModel.deactivationError.value)
    }

    @Test
    fun `sin conexion muestra el error de conexion y permite reintentar`() = runTest(dispatcher) {
        var offline = true
        val service = FakeService { if (offline) throw IOException() else Response.success(Unit) }
        val viewModel = viewModel(service)

        viewModel.confirmDeactivation()
        advanceUntilIdle()
        assertEquals("No se pudo conectar con el servidor", viewModel.deactivationError.value)
        assertFalse(viewModel.deactivated.value)

        offline = false
        viewModel.requestDeactivation()
        assertNull(viewModel.deactivationError.value)
        viewModel.confirmDeactivation()
        advanceUntilIdle()

        assertTrue(viewModel.deactivated.value)
        assertEquals(2, service.deactivateCalls)
    }

    @Test
    fun `descartar el error lo limpia`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeService { failure(404, """{"message":"El animal no existe."}""") })
        viewModel.confirmDeactivation()
        advanceUntilIdle()

        viewModel.dismissDeactivationError()

        assertNull(viewModel.deactivationError.value)
    }
}

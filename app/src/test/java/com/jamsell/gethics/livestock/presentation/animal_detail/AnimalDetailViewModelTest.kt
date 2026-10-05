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

@OptIn(ExperimentalCoroutinesApi::class)
class AnimalDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val dto = AnimalResponse(
        id = "a1", farmId = null, tag = "MX-1", qrCode = "GTH-1", name = null, breed = "Holstein", sex = "FEMALE",
        birthDate = "2022-05-12", initialWeightKg = 650.0, photoUrl = null, status = "ACTIVE"
    )

    private class FakeService(val get: () -> Response<AnimalResponse>) : LivestockService {
        var calls = 0
        override suspend fun getAnimal(animalId: String): Response<AnimalResponse> {
            calls++
            return get()
        }

        override suspend fun updateAnimal(animalId: String, request: UpdateAnimalRequest): Response<AnimalResponse> = error("no usado")
        override suspend fun registerAnimal(request: RegisterAnimalRequest): Response<AnimalResponse> = error("no usado")
        override suspend fun getAnimals(search: String?, status: String?): Response<AnimalListResponse> = error("no usado")
    }

    private class FakeDao(vararg initial: AnimalEntity) : AnimalDao {
        val saved = mutableListOf(*initial)
        override suspend fun insert(animal: AnimalEntity) { saved.removeAll { it.id == animal.id }; saved += animal }
        override suspend fun insertAll(animals: List<AnimalEntity>) { animals.forEach { insert(it) } }
        override suspend fun fetchAll() = saved.toList()
        override suspend fun fetchById(id: String) = saved.firstOrNull { it.id == id }
        override suspend fun search(pattern: String, status: String) = emptyList<AnimalEntity>()
        override suspend fun deleteByStatus(status: String) { saved.removeAll { it.status == status } }
    }

    private fun viewModel(service: FakeService, dao: FakeDao = FakeDao()) =
        AnimalDetailViewModel(LivestockRepository(service, dao), "a1")

    @Test
    fun `load muestra el animal sin error ni offline`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeService { Response.success(dto) })

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("MX-1", state.data?.tag)
        assertEquals("", state.message)
        assertFalse(viewModel.offline.value)
    }

    @Test
    fun `empieza cargando y sin datos`() {
        val viewModel = viewModel(FakeService { Response.success(dto) })

        assertTrue(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.data)
    }

    @Test
    fun `sin conexion muestra el animal guardado y marca offline`() = runTest(dispatcher) {
        val cached = AnimalEntity("a1", "Luna", "MX-1", "Holstein", null, "2022-05-12", 650.0, "GTH-1", "ACTIVE", "FEMALE")
        val viewModel = viewModel(FakeService { throw IOException() }, FakeDao(cached))

        viewModel.load()
        advanceUntilIdle()

        assertEquals("Luna", viewModel.state.value.data?.name)
        assertTrue(viewModel.offline.value)
    }

    @Test
    fun `sin conexion y sin guardado muestra el error`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeService { throw IOException() })

        viewModel.load()
        advanceUntilIdle()

        assertNull(viewModel.state.value.data)
        assertEquals("No se pudo conectar con el servidor", viewModel.state.value.message)
        assertFalse(viewModel.offline.value)
    }

    @Test
    fun `404 muestra el mensaje del backend`() = runTest(dispatcher) {
        val body = """{"message":"El animal no existe."}""".toResponseBody("application/json".toMediaType())
        val viewModel = viewModel(FakeService { Response.error(404, body) })

        viewModel.load()
        advanceUntilIdle()

        assertNull(viewModel.state.value.data)
        assertEquals("El animal no existe.", viewModel.state.value.message)
    }

    @Test
    fun `recargar al volver de editar trae los cambios`() = runTest(dispatcher) {
        var breed = "Holstein"
        val service = FakeService { Response.success(dto.copy(breed = breed)) }
        val viewModel = viewModel(service)

        viewModel.load()
        advanceUntilIdle()
        breed = "Jersey"
        viewModel.load()
        advanceUntilIdle()

        assertEquals("Jersey", viewModel.state.value.data?.breed)
        assertEquals(2, service.calls)
    }

    @Test
    fun `volver a tener conexion limpia el modo offline`() = runTest(dispatcher) {
        var online = false
        val cached = AnimalEntity("a1", "Luna", "MX-1", "Holstein", null)
        val viewModel = viewModel(FakeService { if (online) Response.success(dto) else throw IOException() }, FakeDao(cached))

        viewModel.load()
        advanceUntilIdle()
        assertTrue(viewModel.offline.value)

        online = true
        viewModel.load()
        advanceUntilIdle()

        assertFalse(viewModel.offline.value)
    }
}

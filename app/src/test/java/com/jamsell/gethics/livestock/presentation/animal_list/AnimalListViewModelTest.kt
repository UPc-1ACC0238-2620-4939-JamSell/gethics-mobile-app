package com.jamsell.gethics.livestock.presentation.animal_list

import com.jamsell.gethics.livestock.data.AnimalListResponse
import com.jamsell.gethics.livestock.data.AnimalResponse
import com.jamsell.gethics.livestock.data.LivestockService
import com.jamsell.gethics.livestock.data.RegisterAnimalRequest
import com.jamsell.gethics.livestock.data.UpdateAnimalRequest
import com.jamsell.gethics.livestock.data.local.AnimalDao
import com.jamsell.gethics.livestock.data.local.AnimalEntity
import com.jamsell.gethics.livestock.data.repository.LivestockRepository
import com.jamsell.gethics.livestock.domain.model.AnimalStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
class AnimalListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun dto(id: String, tag: String) = AnimalResponse(
        id = id, farmId = null, tag = tag, qrCode = "GTH-$tag", name = null, breed = "Holstein", sex = null,
        birthDate = "2024-03-01", initialWeightKg = 420.5, photoUrl = null, status = "ACTIVE"
    )

    /** Responde con la funcion recibida y registra cada (search, status) pedido. */
    private class FakeService(val list: (String?, String?) -> Response<AnimalListResponse>) : LivestockService {
        val requests = mutableListOf<Pair<String?, String?>>()
        override suspend fun getAnimals(search: String?, status: String?): Response<AnimalListResponse> {
            requests += search to status
            return list(search, status)
        }

        override suspend fun registerAnimal(request: RegisterAnimalRequest): Response<AnimalResponse> = error("no usado")
        override suspend fun getAnimal(animalId: String): Response<AnimalResponse> = error("no usado")
        override suspend fun updateAnimal(animalId: String, request: UpdateAnimalRequest): Response<AnimalResponse> = error("no usado")
    }

    private class FakeDao(vararg initial: AnimalEntity) : AnimalDao {
        val saved = mutableListOf(*initial)
        override suspend fun insert(animal: AnimalEntity) { saved.removeAll { it.id == animal.id }; saved += animal }
        override suspend fun insertAll(animals: List<AnimalEntity>) { animals.forEach { insert(it) } }
        override suspend fun fetchAll() = saved.toList()
        override suspend fun fetchById(id: String) = saved.firstOrNull { it.id == id }
        override suspend fun search(pattern: String, status: String) = saved.filter { it.status == status }
        override suspend fun deleteByStatus(status: String) { saved.removeAll { it.status == status } }
    }

    private fun ok(vararg animals: AnimalResponse) = Response.success(AnimalListResponse(animals.toList(), null))

    private fun viewModel(service: FakeService, dao: FakeDao = FakeDao()) =
        AnimalListViewModel(LivestockRepository(service, dao))

    @Test
    fun `load muestra los animales, el total y sin mensaje`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeService { _, _ -> ok(dto("1", "MX-1"), dto("2", "MX-2")) })

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(listOf("MX-1", "MX-2"), state.data?.map { it.tag })
        assertEquals("", state.message)
        assertEquals(2, viewModel.total.value)
        assertFalse(viewModel.offline.value)
    }

    @Test
    fun `lista vacia sin busqueda muestra No hay animales registrados`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeService { _, _ -> ok() })

        viewModel.load()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.data!!.isEmpty())
        assertEquals(NO_ANIMALS, viewModel.state.value.message)
        assertEquals(0, viewModel.total.value)
    }

    @Test
    fun `busqueda sin coincidencias muestra Sin resultados`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeService { _, _ -> ok() })

        viewModel.onQueryChange("zzz")
        advanceUntilIdle()

        assertEquals(NO_RESULTS, viewModel.state.value.message)
    }

    @Test
    fun `la busqueda espera el debounce y solo consulta con el ultimo texto`() = runTest(dispatcher) {
        val service = FakeService { _, _ -> ok() }
        val viewModel = viewModel(service)

        viewModel.onQueryChange("h")
        advanceTimeBy(100)
        viewModel.onQueryChange("hol")
        advanceTimeBy(SEARCH_DEBOUNCE_MS - 1)
        runCurrent()
        assertTrue(service.requests.isEmpty())

        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals(listOf<Pair<String?, String?>>("hol" to "ACTIVE"), service.requests)
    }

    @Test
    fun `cambiar el estado consulta de inmediato con ese estado`() = runTest(dispatcher) {
        val service = FakeService { _, _ -> ok() }
        val viewModel = viewModel(service)

        viewModel.onStatusChange(AnimalStatus.SOLD)
        advanceUntilIdle()

        assertEquals(AnimalStatus.SOLD, viewModel.status.value)
        assertEquals(listOf<Pair<String?, String?>>(null to "SOLD"), service.requests)
    }

    @Test
    fun `elegir el mismo estado no vuelve a consultar`() = runTest(dispatcher) {
        val service = FakeService { _, _ -> ok() }
        val viewModel = viewModel(service)

        viewModel.onStatusChange(AnimalStatus.ACTIVE)
        advanceUntilIdle()

        assertTrue(service.requests.isEmpty())
    }

    @Test
    fun `el total del hato no cambia al buscar`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeService { search, _ -> if (search == null) ok(dto("1", "MX-1"), dto("2", "MX-2")) else ok(dto("1", "MX-1")) })

        viewModel.load()
        advanceUntilIdle()
        viewModel.onQueryChange("mx-1")
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.data?.size)
        assertEquals(2, viewModel.total.value)
    }

    @Test
    fun `sin conexion muestra los guardados y marca offline`() = runTest(dispatcher) {
        val cached = AnimalEntity("1", "MX-1", "MX-1", "Holstein", null, "2024-03-01", 420.5, "GTH-1", "ACTIVE")
        val viewModel = viewModel(FakeService { _, _ -> throw IOException() }, FakeDao(cached))

        viewModel.load()
        advanceUntilIdle()

        assertEquals(listOf("MX-1"), viewModel.state.value.data?.map { it.tag })
        assertTrue(viewModel.offline.value)
    }

    @Test
    fun `sin conexion y sin guardados muestra la lista vacia en modo offline`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeService { _, _ -> throw IOException() })

        viewModel.load()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.data!!.isEmpty())
        assertEquals(NO_ANIMALS, viewModel.state.value.message)
        assertTrue(viewModel.offline.value)
    }

    @Test
    fun `error del backend muestra el mensaje sin datos`() = runTest(dispatcher) {
        val body = """{"message":"Parametro invalido: status"}""".toResponseBody("application/json".toMediaType())
        val viewModel = viewModel(FakeService { _, _ -> Response.error(400, body) })

        viewModel.load()
        advanceUntilIdle()

        assertNull(viewModel.state.value.data)
        assertEquals("Parametro invalido: status", viewModel.state.value.message)
        assertFalse(viewModel.offline.value)
    }

    @Test
    fun `volver a tener conexion limpia el modo offline`() = runTest(dispatcher) {
        var online = false
        val viewModel = viewModel(FakeService { _, _ -> if (online) ok(dto("1", "MX-1")) else throw IOException() })

        viewModel.load()
        advanceUntilIdle()
        assertTrue(viewModel.offline.value)

        online = true
        viewModel.load()
        advanceUntilIdle()

        assertFalse(viewModel.offline.value)
        assertEquals(listOf("MX-1"), viewModel.state.value.data?.map { it.tag })
    }

    @Test
    fun `peso se muestra sin decimales si es entero`() {
        assertEquals("542", formatWeight(542.0))
        assertEquals("420.5", formatWeight(420.5))
    }
}

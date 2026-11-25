package com.jamsell.gethics.livestock.presentation.animal_detail

import com.jamsell.gethics.livestock.data.AnimalListResponse
import com.jamsell.gethics.livestock.data.AnimalResponse
import com.jamsell.gethics.livestock.data.FarmResponse
import com.jamsell.gethics.livestock.data.FarmService
import com.jamsell.gethics.livestock.data.LivestockService
import com.jamsell.gethics.livestock.data.RegisterAnimalRequest
import com.jamsell.gethics.livestock.data.UpdateAnimalRequest
import com.jamsell.gethics.livestock.data.local.AnimalDao
import com.jamsell.gethics.livestock.data.local.AnimalEntity
import com.jamsell.gethics.livestock.data.repository.FarmRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/** US-43: la granja asignada se trae despues del animal y un fallo no rompe la ficha. */
@OptIn(ExperimentalCoroutinesApi::class)
class AnimalDetailViewModelFarmTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun animal(farmId: String?) = AnimalResponse(
        id = "a1", farmId = farmId, tag = "MX-1", qrCode = "GTH-1", name = null, breed = "Holstein", sex = "FEMALE",
        birthDate = "2022-05-12", initialWeightKg = 650.0, photoUrl = null, status = "ACTIVE"
    )

    private class FakeLivestockService(private val dto: AnimalResponse) : LivestockService {
        override suspend fun getAnimal(animalId: String): Response<AnimalResponse> = Response.success(dto)
        override suspend fun deactivateAnimal(animalId: String): Response<Unit> = error("no usado")
        override suspend fun updateAnimal(animalId: String, request: UpdateAnimalRequest): Response<AnimalResponse> = error("no usado")
        override suspend fun registerAnimal(request: RegisterAnimalRequest): Response<AnimalResponse> = error("no usado")
        override suspend fun getAnimals(search: String?, status: String?): Response<AnimalListResponse> = error("no usado")
    }

    private class FakeFarmService(private val reply: () -> Response<FarmResponse>) : FarmService {
        val requested = mutableListOf<String>()
        override suspend fun getFarm(farmId: String): Response<FarmResponse> {
            requested += farmId
            return reply()
        }
    }

    private class FakeDao : AnimalDao {
        private val saved = mutableListOf<AnimalEntity>()
        override suspend fun insert(animal: AnimalEntity) { saved.removeAll { it.id == animal.id }; saved += animal }
        override suspend fun insertAll(animals: List<AnimalEntity>) { animals.forEach { insert(it) } }
        override suspend fun fetchAll() = saved.toList()
        override suspend fun fetchById(id: String) = saved.firstOrNull { it.id == id }
        override suspend fun search(pattern: String, status: String) = emptyList<AnimalEntity>()
        override suspend fun deleteByStatus(status: String) { saved.removeAll { it.status == status } }
    }

    private val farmDto = FarmResponse("f1", "o1", "Fundo Sur", "Jauja, Junin", 12.5, "ACTIVE")

    private fun viewModel(animalDto: AnimalResponse, farmService: FakeFarmService) = AnimalDetailViewModel(
        LivestockRepository(FakeLivestockService(animalDto), FakeDao()),
        "a1",
        FarmRepository(farmService)
    )

    @Test
    fun `load trae la granja asignada del animal`() = runTest(dispatcher) {
        val farmService = FakeFarmService { Response.success(farmDto) }
        val viewModel = viewModel(animal("f1"), farmService)

        viewModel.load()
        advanceUntilIdle()

        assertEquals("Fundo Sur", viewModel.farm.value?.name)
        assertEquals(listOf("f1"), farmService.requested)
    }

    @Test
    fun `un animal sin granja no consulta granjas`() = runTest(dispatcher) {
        val farmService = FakeFarmService { Response.success(farmDto) }
        val viewModel = viewModel(animal(null), farmService)

        viewModel.load()
        advanceUntilIdle()

        assertNull(viewModel.farm.value)
        assertTrue(farmService.requested.isEmpty())
    }

    @Test
    fun `si la granja falla el animal igual se muestra`() = runTest(dispatcher) {
        val notFound = Response.error<FarmResponse>(404, "{}".toResponseBody("application/json".toMediaType()))
        val viewModel = viewModel(animal("f1"), FakeFarmService { notFound })

        viewModel.load()
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.data)
        assertNull(viewModel.state.value.message)
        assertNull(viewModel.farm.value)
    }

    @Test
    fun `sin conexion la granja queda vacia y el animal se muestra`() = runTest(dispatcher) {
        val viewModel = viewModel(animal("f1"), FakeFarmService { throw IOException("sin red") })

        viewModel.load()
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.data)
        assertNull(viewModel.farm.value)
    }

    @Test
    fun `recargar con la misma granja no la vuelve a pedir`() = runTest(dispatcher) {
        val farmService = FakeFarmService { Response.success(farmDto) }
        val viewModel = viewModel(animal("f1"), farmService)

        viewModel.load()
        advanceUntilIdle()
        viewModel.load()
        advanceUntilIdle()

        assertEquals(listOf("f1"), farmService.requested)
    }
}

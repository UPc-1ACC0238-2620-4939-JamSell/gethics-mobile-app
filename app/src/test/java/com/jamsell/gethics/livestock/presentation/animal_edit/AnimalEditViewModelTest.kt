package com.jamsell.gethics.livestock.presentation.animal_edit

import com.jamsell.gethics.livestock.data.AnimalListResponse
import com.jamsell.gethics.livestock.data.AnimalResponse
import com.jamsell.gethics.livestock.data.LivestockService
import com.jamsell.gethics.livestock.data.RegisterAnimalRequest
import com.jamsell.gethics.livestock.data.UpdateAnimalRequest
import com.jamsell.gethics.livestock.data.local.AnimalDao
import com.jamsell.gethics.livestock.data.local.AnimalEntity
import com.jamsell.gethics.livestock.data.repository.LivestockRepository
import com.jamsell.gethics.livestock.presentation.animal_register.FUTURE_BIRTH_DATE_ERROR
import com.jamsell.gethics.livestock.presentation.animal_register.REQUIRED_FIELD_ERROR
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AnimalEditViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val today = LocalDate.of(2026, 10, 5)

    private val dto = AnimalResponse(
        id = "a1", farmId = "farm-1", tag = "MX-1", qrCode = "GTH-1", name = "Luna", breed = "Holstein", sex = "FEMALE",
        birthDate = "2022-05-12", initialWeightKg = 650.0, photoUrl = null, status = "ACTIVE"
    )

    private class FakeService(
        val get: () -> Response<AnimalResponse>,
        val update: (UpdateAnimalRequest) -> Response<AnimalResponse> = { error("no usado") }
    ) : LivestockService {
        var updates = 0
        var sent: UpdateAnimalRequest? = null

        override suspend fun getAnimal(animalId: String): Response<AnimalResponse> = get()

        override suspend fun updateAnimal(animalId: String, request: UpdateAnimalRequest): Response<AnimalResponse> {
            updates++
            sent = request
            return update(request)
        }

        override suspend fun registerAnimal(request: RegisterAnimalRequest): Response<AnimalResponse> = error("no usado")
        override suspend fun getAnimals(search: String?, status: String?): Response<AnimalListResponse> = error("no usado")
    }

    private class NoopDao : AnimalDao {
        override suspend fun insert(animal: AnimalEntity) = Unit
        override suspend fun insertAll(animals: List<AnimalEntity>) = Unit
        override suspend fun fetchAll() = emptyList<AnimalEntity>()
        override suspend fun fetchById(id: String): AnimalEntity? = null
        override suspend fun search(pattern: String, status: String) = emptyList<AnimalEntity>()
        override suspend fun deleteByStatus(status: String) = Unit
    }

    private fun viewModel(service: FakeService, clock: Clock = Clock.fixed(Instant.parse("2026-10-05T15:00:00Z"), ZoneId.of("America/Lima"))) =
        AnimalEditViewModel(LivestockRepository(service, NoopDao()), "a1", clock)

    private fun okService(update: (UpdateAnimalRequest) -> Response<AnimalResponse> = { Response.success(dto.copy(name = "Estrella")) }) =
        FakeService(get = { Response.success(dto) }, update = update)

    private fun errorBody(message: String) = """{"message":"$message"}""".toResponseBody("application/json".toMediaType())

    // ---------------- validacion ----------------

    @Test
    fun `raza vacia y fecha faltante marcan obligatorio`() {
        val errors = validateAnimalEdit("  ", null, today)

        assertEquals(REQUIRED_FIELD_ERROR, errors.breed)
        assertEquals(REQUIRED_FIELD_ERROR, errors.birthDate)
    }

    @Test
    fun `fecha futura es invalida y hoy es valida`() {
        assertEquals(FUTURE_BIRTH_DATE_ERROR, validateAnimalEdit("Jersey", today.plusDays(1), today).birthDate)
        assertFalse(validateAnimalEdit("Jersey", today, today).hasErrors)
    }

    // ---------------- carga ----------------

    @Test
    fun `init carga el animal para precargar el formulario`() = runTest(dispatcher) {
        val viewModel = viewModel(okService())

        assertTrue(viewModel.animal.value.isLoading)
        advanceUntilIdle()

        assertEquals("MX-1", viewModel.animal.value.data?.tag)
        assertFalse(viewModel.animal.value.isLoading)
    }

    @Test
    fun `sin conexion no se puede editar y se muestra el error`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeService(get = { throw IOException() }))

        advanceUntilIdle()

        assertNull(viewModel.animal.value.data)
        assertEquals("No se pudo conectar con el servidor", viewModel.animal.value.message)
    }

    @Test
    fun `404 al cargar muestra el mensaje del backend`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeService(get = { Response.error(404, errorBody("El animal no existe.")) }))

        advanceUntilIdle()

        assertNull(viewModel.animal.value.data)
        assertEquals("El animal no existe.", viewModel.animal.value.message)
    }

    // ---------------- guardar ----------------

    @Test
    fun `guardar con datos validos llama al backend y avisa que se guardo`() = runTest(dispatcher) {
        val service = okService()
        val viewModel = viewModel(service)
        advanceUntilIdle()

        viewModel.save("Estrella", "Jersey", "MALE", LocalDate.of(2023, 1, 2))
        advanceUntilIdle()

        assertEquals(true, viewModel.saveState.value.data)
        assertEquals(1, service.updates)
        assertEquals("Jersey", service.sent?.breed)
        assertEquals("2023-01-02", service.sent?.birthDate)
        assertEquals("MALE", service.sent?.sex)
    }

    @Test
    fun `guardar conserva peso y granja del animal cargado`() = runTest(dispatcher) {
        val service = okService()
        val viewModel = viewModel(service)
        advanceUntilIdle()

        viewModel.save("Luna", "Holstein", "FEMALE", LocalDate.of(2022, 5, 12))
        advanceUntilIdle()

        assertEquals(650.0, service.sent?.initialWeightKg ?: 0.0, 0.0)
        assertEquals("farm-1", service.sent?.farmId)
    }

    @Test
    fun `nombre en blanco se envia como null y el normal sin espacios`() = runTest(dispatcher) {
        val service = okService()
        val viewModel = viewModel(service)
        advanceUntilIdle()

        viewModel.save("   ", "Holstein", null, LocalDate.of(2022, 5, 12))
        advanceUntilIdle()
        assertNull(service.sent?.name)

        viewModel.save("  Estrella ", "Holstein", null, LocalDate.of(2022, 5, 12))
        advanceUntilIdle()
        assertEquals("Estrella", service.sent?.name)
    }

    @Test
    fun `guardar con raza vacia no llama al backend`() = runTest(dispatcher) {
        val service = okService()
        val viewModel = viewModel(service)
        advanceUntilIdle()

        viewModel.save("Luna", "  ", "FEMALE", LocalDate.of(2022, 5, 12))
        advanceUntilIdle()

        assertEquals(REQUIRED_FIELD_ERROR, viewModel.errors.value.breed)
        assertEquals(0, service.updates)
        assertNull(viewModel.saveState.value.data)
    }

    @Test
    fun `fecha futura segun el clock no llama al backend`() = runTest(dispatcher) {
        val service = okService()
        val viewModel = viewModel(service)
        advanceUntilIdle()

        // Hora de Lima: 2026-10-05 10:00. Manana ya es futuro.
        viewModel.save("Luna", "Holstein", "FEMALE", LocalDate.of(2026, 10, 6))
        advanceUntilIdle()

        assertEquals(FUTURE_BIRTH_DATE_ERROR, viewModel.errors.value.birthDate)
        assertEquals(0, service.updates)
    }

    @Test
    fun `error del backend al guardar muestra su mensaje`() = runTest(dispatcher) {
        val viewModel = viewModel(okService { Response.error(400, errorBody("La raza es obligatoria.")) })
        advanceUntilIdle()

        viewModel.save("Luna", "Holstein", "FEMALE", LocalDate.of(2022, 5, 12))
        advanceUntilIdle()

        assertEquals("La raza es obligatoria.", viewModel.saveState.value.message)
        assertNull(viewModel.saveState.value.data)
        assertFalse(viewModel.saveState.value.isLoading)
    }

    @Test
    fun `guardar sin conexion muestra el error de conexion`() = runTest(dispatcher) {
        val viewModel = viewModel(okService { throw IOException() })
        advanceUntilIdle()

        viewModel.save("Luna", "Holstein", "FEMALE", LocalDate.of(2022, 5, 12))
        advanceUntilIdle()

        assertEquals("No se pudo conectar con el servidor", viewModel.saveState.value.message)
    }

    @Test
    fun `guardar antes de cargar el animal no hace nada`() = runTest(dispatcher) {
        val service = okService()
        val viewModel = viewModel(service)

        viewModel.save("Luna", "Holstein", "FEMALE", LocalDate.of(2022, 5, 12))
        advanceUntilIdle()

        assertEquals(0, service.updates)
    }
}

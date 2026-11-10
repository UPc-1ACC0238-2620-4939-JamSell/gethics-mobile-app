package com.jamsell.gethics.livestock.presentation.animal_list

import com.jamsell.gethics.livestock.data.AnimalListResponse
import com.jamsell.gethics.livestock.data.AnimalResponse
import com.jamsell.gethics.livestock.data.AnimalSaleResponse
import com.jamsell.gethics.livestock.data.LivestockService
import com.jamsell.gethics.livestock.data.RegisterAnimalRequest
import com.jamsell.gethics.livestock.data.SellAnimalRequest
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

/** US-29 en la lista de animales: buscar un animal por su QR y abrir su ficha. */
@OptIn(ExperimentalCoroutinesApi::class)
class AnimalListQrTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeService(val qr: () -> Response<AnimalResponse>) : LivestockService {
        var codes = mutableListOf<String>()

        override suspend fun getAnimalByQr(code: String): Response<AnimalResponse> {
            codes += code
            return qr()
        }

        override suspend fun getAnimals(search: String?, status: String?): Response<AnimalListResponse> =
            Response.success(AnimalListResponse(emptyList(), "No hay animales registrados."))

        override suspend fun sellAnimal(animalId: String, request: SellAnimalRequest): Response<AnimalSaleResponse> = error("no usado")
        override suspend fun deactivateAnimal(animalId: String): Response<Unit> = error("no usado")
        override suspend fun getAnimal(animalId: String): Response<AnimalResponse> = error("no usado")
        override suspend fun updateAnimal(animalId: String, request: UpdateAnimalRequest): Response<AnimalResponse> = error("no usado")
        override suspend fun registerAnimal(request: RegisterAnimalRequest): Response<AnimalResponse> = error("no usado")
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

    private fun body() = AnimalResponse("a1", "farm-1", "MX-1", "GTH-1", "Luna", "Holstein", "FEMALE", "2022-05-12", 650.0, null, "ACTIVE")

    private fun viewModel(service: FakeService, dao: FakeDao = FakeDao()) = AnimalListViewModel(LivestockRepository(service, dao))

    @Test
    fun `abrir y cerrar el dialogo no busca nada`() {
        val service = FakeService { Response.success(body()) }
        val viewModel = viewModel(service)

        viewModel.openQrDialog()
        assertTrue(viewModel.qrDialog.value)

        viewModel.closeQrDialog()
        assertFalse(viewModel.qrDialog.value)
        assertTrue(service.codes.isEmpty())
    }

    @Test
    fun `un codigo vacio muestra el error y no llama al backend`() = runTest(dispatcher) {
        val service = FakeService { Response.success(body()) }
        val viewModel = viewModel(service)
        viewModel.openQrDialog()

        viewModel.lookUpQr("   ")
        advanceUntilIdle()

        assertEquals(QR_REQUIRED_ERROR, viewModel.qrError.value)
        assertTrue(service.codes.isEmpty())
        assertTrue(viewModel.qrDialog.value)
    }

    @Test
    fun `QR encontrado cierra el dialogo y entrega el id del animal para abrir su ficha`() = runTest(dispatcher) {
        val service = FakeService { Response.success(body()) }
        val viewModel = viewModel(service)
        viewModel.openQrDialog()

        viewModel.lookUpQr("GTH-1")
        advanceUntilIdle()

        assertEquals("a1", viewModel.qrResult.value)
        assertFalse(viewModel.qrDialog.value)
        assertFalse(viewModel.qrLooking.value)
        assertNull(viewModel.qrError.value)

        viewModel.consumeQrResult()
        assertNull(viewModel.qrResult.value)
    }

    @Test
    fun `QR inexistente muestra el message del backend y deja el dialogo abierto`() = runTest(dispatcher) {
        val service = FakeService {
            Response.error(404, """{"message":"No hay ningun animal con ese codigo QR."}""".toResponseBody("application/json".toMediaType()))
        }
        val viewModel = viewModel(service)
        viewModel.openQrDialog()

        viewModel.lookUpQr("GTH-X")
        advanceUntilIdle()

        assertEquals("No hay ningun animal con ese codigo QR.", viewModel.qrError.value)
        assertNull(viewModel.qrResult.value)
        assertTrue(viewModel.qrDialog.value)
    }

    @Test
    fun `sin conexion un animal guardado en el telefono se abre igual`() = runTest(dispatcher) {
        val dao = FakeDao(AnimalEntity("a1", "Luna", "MX-1", "Holstein", null, "2022-05-12", 650.0, "GTH-1", "ACTIVE", "FEMALE", "farm-1"))
        val viewModel = viewModel(FakeService { throw IOException() }, dao)
        viewModel.openQrDialog()

        viewModel.lookUpQr("GTH-1")
        advanceUntilIdle()

        assertEquals("a1", viewModel.qrResult.value)
        assertNull(viewModel.qrError.value)
    }

    @Test
    fun `sin conexion y sin copia guardada muestra el error de conexion`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeService { throw IOException() })
        viewModel.openQrDialog()

        viewModel.lookUpQr("GTH-1")
        advanceUntilIdle()

        assertNull(viewModel.qrResult.value)
        assertEquals("No se pudo conectar con el servidor", viewModel.qrError.value)
    }
}

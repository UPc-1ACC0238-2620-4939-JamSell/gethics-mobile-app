package com.jamsell.gethics.livestock.presentation.animal_detail

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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** US-30: validacion del formulario de venta y flujo en la ficha del animal. */
@OptIn(ExperimentalCoroutinesApi::class)
class AnimalSaleTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 11, 10)
    private val clock = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeService(val sale: () -> Response<AnimalSaleResponse>) : LivestockService {
        var sales = mutableListOf<SellAnimalRequest>()

        override suspend fun sellAnimal(animalId: String, request: SellAnimalRequest): Response<AnimalSaleResponse> {
            sales += request
            return sale()
        }

        override suspend fun getAnimal(animalId: String): Response<AnimalResponse> =
            Response.success(AnimalResponse("a1", "farm-1", "MX-1", "GTH-1", "Luna", "Holstein", "FEMALE", "2022-05-12", 650.0, null, "ACTIVE"))

        override suspend fun getAnimalByQr(code: String): Response<AnimalResponse> = error("no usado")
        override suspend fun deactivateAnimal(animalId: String): Response<Unit> = error("no usado")
        override suspend fun updateAnimal(animalId: String, request: UpdateAnimalRequest): Response<AnimalResponse> = error("no usado")
        override suspend fun registerAnimal(request: RegisterAnimalRequest): Response<AnimalResponse> = error("no usado")
        override suspend fun getAnimals(search: String?, status: String?): Response<AnimalListResponse> = error("no usado")
    }

    private class FakeDao : AnimalDao {
        val saved = mutableListOf<AnimalEntity>()
        override suspend fun insert(animal: AnimalEntity) { saved.removeAll { it.id == animal.id }; saved += animal }
        override suspend fun insertAll(animals: List<AnimalEntity>) { animals.forEach { insert(it) } }
        override suspend fun fetchAll() = saved.toList()
        override suspend fun fetchById(id: String) = saved.firstOrNull { it.id == id }
        override suspend fun search(pattern: String, status: String) = emptyList<AnimalEntity>()
        override suspend fun deleteByStatus(status: String) { saved.removeAll { it.status == status } }
    }

    private fun saleBody() = AnimalSaleResponse("a1", "SOLD", 1500.0, "2026-11-10", "tx-1")

    private fun viewModel(service: FakeService, dao: FakeDao = FakeDao()) =
        AnimalDetailViewModel(LivestockRepository(service, dao), "a1", "owner-1", clock)

    // ---------------- validacion pura ----------------

    @Test
    fun `parsePrice acepta punto y coma, hasta 2 decimales y mayor a cero`() {
        assertEquals(1500.5, parsePrice("1500.5")!!, 0.0)
        assertEquals(1500.5, parsePrice(" 1500,50 ")!!, 0.0)
        assertEquals(0.01, parsePrice("0.01")!!, 0.0)
        assertNull(parsePrice(""))
        assertNull(parsePrice("0"))
        assertNull(parsePrice("-5"))
        assertNull(parsePrice("10.999"))
        assertNull(parsePrice("abc"))
        assertNull(parsePrice("99999999999"))
    }

    @Test
    fun `validateSale pide precio valido y una fecha valida que no sea futura`() {
        assertFalse(validateSale("1500", "2026-11-10", today).hasErrors)
        assertFalse(validateSale("1500", "2026-01-01", today).hasErrors)
        assertEquals(INVALID_PRICE_ERROR, validateSale("", "2026-11-10", today).price)
        assertEquals(INVALID_SALE_DATE_ERROR, validateSale("10", "10/11/2026", today).date)
        assertEquals(INVALID_SALE_DATE_ERROR, validateSale("10", "", today).date)
        assertEquals(FUTURE_SALE_DATE_ERROR, validateSale("10", "2026-11-11", today).date)
    }

    // ---------------- ViewModel ----------------

    @Test
    fun `el formulario propone la fecha de hoy`() {
        assertEquals("2026-11-10", viewModel(FakeService { saleBody().let { Response.success(it) } }).defaultSaleDate())
    }

    @Test
    fun `pedir la venta solo abre el formulario y no llama al backend`() = runTest(dispatcher) {
        val service = FakeService { Response.success(saleBody()) }
        val viewModel = viewModel(service)
        viewModel.load()
        advanceUntilIdle()

        viewModel.requestSale()
        advanceUntilIdle()

        assertTrue(viewModel.sellingForm.value)
        assertTrue(service.sales.isEmpty())
    }

    @Test
    fun `cancelar cierra el formulario sin vender`() = runTest(dispatcher) {
        val service = FakeService { Response.success(saleBody()) }
        val viewModel = viewModel(service)
        viewModel.load()
        advanceUntilIdle()
        viewModel.requestSale()

        viewModel.cancelSale()
        advanceUntilIdle()

        assertFalse(viewModel.sellingForm.value)
        assertFalse(viewModel.sold.value)
        assertTrue(service.sales.isEmpty())
    }

    @Test
    fun `con datos invalidos el formulario sigue abierto y no se llama al backend`() = runTest(dispatcher) {
        val service = FakeService { Response.success(saleBody()) }
        val viewModel = viewModel(service)
        viewModel.load()
        advanceUntilIdle()
        viewModel.requestSale()

        viewModel.confirmSale("0", "2026-12-01")
        advanceUntilIdle()

        assertTrue(viewModel.sellingForm.value)
        assertEquals(INVALID_PRICE_ERROR, viewModel.saleErrors.value.price)
        assertEquals(FUTURE_SALE_DATE_ERROR, viewModel.saleErrors.value.date)
        assertTrue(service.sales.isEmpty())
        assertFalse(viewModel.sold.value)
    }

    @Test
    fun `confirmar con datos validos registra la venta, deja el animal SOLD y avisa a la pantalla`() = runTest(dispatcher) {
        val service = FakeService { Response.success(saleBody()) }
        val dao = FakeDao()
        val viewModel = viewModel(service, dao)
        viewModel.load()
        advanceUntilIdle()
        viewModel.requestSale()

        viewModel.confirmSale("1500,5", "2026-11-10")
        advanceUntilIdle()

        assertTrue(viewModel.sold.value)
        assertFalse(viewModel.sellingForm.value)
        assertFalse(viewModel.selling.value)
        assertNull(viewModel.saleError.value)
        assertEquals(SellAnimalRequest(1500.5, "2026-11-10", null), service.sales.single())
        assertEquals("SOLD", dao.saved.single().status)
    }

    @Test
    fun `409 muestra el message del backend y no avisa de venta`() = runTest(dispatcher) {
        val service = FakeService {
            Response.error(409, """{"message":"El animal ya no esta activo."}""".toResponseBody("application/json".toMediaType()))
        }
        val viewModel = viewModel(service)
        viewModel.load()
        advanceUntilIdle()
        viewModel.requestSale()

        viewModel.confirmSale("100", "2026-11-10")
        advanceUntilIdle()

        assertFalse(viewModel.sold.value)
        assertEquals("El animal ya no esta activo.", viewModel.saleError.value)
        assertFalse(viewModel.selling.value)

        viewModel.dismissSaleError()
        assertNull(viewModel.saleError.value)
    }
}

package com.jamsell.gethics.livestock.presentation.animal_register

import com.jamsell.gethics.livestock.data.AnimalResponse
import com.jamsell.gethics.livestock.data.LivestockService
import com.jamsell.gethics.livestock.data.RegisterAnimalRequest
import com.jamsell.gethics.livestock.data.local.AnimalDao
import com.jamsell.gethics.livestock.data.local.AnimalEntity
import com.jamsell.gethics.livestock.data.repository.LivestockRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.time.LocalDate

class AnimalRegisterViewModelTest {

    private val today = LocalDate.of(2026, 10, 5)

    private class CountingService : LivestockService {
        var calls = 0
        override suspend fun registerAnimal(request: RegisterAnimalRequest): Response<AnimalResponse> {
            calls++
            error("no usado")
        }
    }

    private object NoopDao : AnimalDao {
        override suspend fun insert(animal: AnimalEntity) = Unit
        override suspend fun insertAll(animals: List<AnimalEntity>) = Unit
        override suspend fun fetchAll() = emptyList<AnimalEntity>()
        override suspend fun fetchById(id: String): AnimalEntity? = null
    }

    @Test
    fun `arete raza y fecha vacios marcan obligatorio y el peso es opcional`() {
        val errors = validateAnimalForm("  ", "", null, "", today)
        assertEquals(REQUIRED_FIELD_ERROR, errors.tag)
        assertEquals(REQUIRED_FIELD_ERROR, errors.breed)
        assertEquals(REQUIRED_FIELD_ERROR, errors.birthDate)
        assertNull(errors.weight)
    }

    @Test
    fun `fecha de nacimiento futura es invalida y hoy es valida`() {
        assertEquals(FUTURE_BIRTH_DATE_ERROR, validateAnimalForm("A", "Jersey", today.plusDays(1), "", today).birthDate)
        assertNull(validateAnimalForm("A", "Jersey", today, "", today).birthDate)
    }

    @Test
    fun `peso debe ser un numero mayor a 0 y acepta coma decimal`() {
        assertEquals(INVALID_WEIGHT_ERROR, validateAnimalForm("A", "Jersey", today, "abc", today).weight)
        assertEquals(INVALID_WEIGHT_ERROR, validateAnimalForm("A", "Jersey", today, "0", today).weight)
        assertNull(validateAnimalForm("A", "Jersey", today, "350,5", today).weight)
        assertEquals(350.5, parseWeight("350,5")!!, 0.0)
    }

    @Test
    fun `formulario completo no tiene errores`() =
        assertFalse(validateAnimalForm("MX-00123", "Holstein", today, "420", today).hasErrors)

    @Test
    fun `save con campos invalidos no llama al backend ni carga`() {
        val service = CountingService()
        val viewModel = AnimalRegisterViewModel(LivestockRepository(service, NoopDao))

        viewModel.save("", "Holstein", LocalDate.of(2024, 1, 1), "", null)

        assertEquals(REQUIRED_FIELD_ERROR, viewModel.errors.value.tag)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(0, service.calls)
    }

    @Test
    fun `save con fecha futura segun el clock no llama al backend`() {
        val service = CountingService()
        val clock = java.time.Clock.fixed(
            java.time.Instant.parse("2026-10-05T00:27:00Z"), java.time.ZoneId.of("America/Lima")
        )
        val viewModel = AnimalRegisterViewModel(LivestockRepository(service, NoopDao), clock)

        // Hora de Lima: 2026-10-04 19:27, asi que el 5 todavia es futuro aunque en UTC ya sea hoy.
        viewModel.save("MX-1", "Holstein", LocalDate.of(2026, 10, 5), "", null)

        assertEquals(FUTURE_BIRTH_DATE_ERROR, viewModel.errors.value.birthDate)
        assertTrue(viewModel.errors.value.hasErrors)
        assertEquals(0, service.calls)
    }
}

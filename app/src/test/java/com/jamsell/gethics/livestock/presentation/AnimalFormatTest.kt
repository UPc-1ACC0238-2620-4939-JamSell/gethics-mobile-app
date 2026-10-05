package com.jamsell.gethics.livestock.presentation

import com.jamsell.gethics.livestock.domain.model.Animal
import com.jamsell.gethics.livestock.presentation.animal_detail.summaryLine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AnimalFormatTest {

    private val today = LocalDate.of(2026, 10, 5)

    @Test
    fun `edad en anios`() {
        assertEquals("4 años", formatAge("2022-05-12", today))
        assertEquals("1 año", formatAge("2025-10-05", today))
        assertEquals("1 año", formatAge("2025-10-04", today))
    }

    @Test
    fun `un dia antes del cumple todavia no cuenta el anio`() {
        assertEquals("11 meses", formatAge("2025-10-06", today))
    }

    @Test
    fun `edad en meses cuando es menor a un anio`() {
        assertEquals("1 mes", formatAge("2026-09-05", today))
        assertEquals("8 meses", formatAge("2026-02-05", today))
    }

    @Test
    fun `menos de un mes`() {
        assertEquals("Menos de 1 mes", formatAge("2026-09-20", today))
        assertEquals("Menos de 1 mes", formatAge("2026-10-05", today))
    }

    @Test
    fun `edad sin fecha valida o futura no se inventa`() {
        assertEquals(NO_DATA, formatAge(null, today))
        assertEquals(NO_DATA, formatAge("no-es-fecha", today))
        assertEquals(NO_DATA, formatAge("2026-10-06", today))
    }

    @Test
    fun `fecha de nacimiento como dd mm yyyy`() {
        assertEquals("12/05/2022", formatBirthDate("2022-05-12"))
        assertEquals("01/01/2024", formatBirthDate("2024-01-01"))
        assertEquals(NO_DATA, formatBirthDate(null))
        assertEquals(NO_DATA, formatBirthDate("x"))
    }

    @Test
    fun `sexo en espaniol y sin especificar si falta o es desconocido`() {
        assertEquals("Macho", sexLabel("MALE"))
        assertEquals("Hembra", sexLabel("FEMALE"))
        assertEquals(NO_SEX, sexLabel(null))
        assertEquals(NO_SEX, sexLabel("OTRO"))
    }

    private fun animal(sex: String?, birthDate: String?) =
        Animal(id = "1", name = "MX-1", tag = "MX-1", breed = "Holstein", birthDate = birthDate, sex = sex)

    @Test
    fun `resumen con raza sexo y edad`() {
        assertEquals("HOLSTEIN · HEMBRA · 4 AÑOS", summaryLine(animal("FEMALE", "2022-05-12"), today))
    }

    @Test
    fun `el resumen omite lo que no se conoce`() {
        assertEquals("HOLSTEIN · 4 AÑOS", summaryLine(animal(null, "2022-05-12"), today))
        assertEquals("HOLSTEIN · MACHO", summaryLine(animal("MALE", null), today))
        assertEquals("HOLSTEIN", summaryLine(animal(null, null), today))
    }
}

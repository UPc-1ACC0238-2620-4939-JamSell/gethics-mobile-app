package com.jamsell.gethics.iot.presentation.readings

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.Locale
import java.util.TimeZone

class ReadingsDatesTest {

    private val original = TimeZone.getDefault()

    @Before
    fun setUp() = Unit

    @After
    fun tearDown() = TimeZone.setDefault(original)

    @Test
    fun `ida y vuelta con el DatePicker no desplaza el dia en ninguna zona horaria`() {
        val days = listOf(LocalDate.of(2026, 11, 27), LocalDate.of(2026, 1, 1), LocalDate.of(2028, 2, 29), LocalDate.of(2026, 12, 31))
        for (zone in listOf("UTC", "America/Lima", "America/Los_Angeles", "Asia/Tokyo", "Pacific/Kiritimati", "Pacific/Pago_Pago")) {
            TimeZone.setDefault(TimeZone.getTimeZone(zone))
            for (day in days) assertEquals("zona $zone", day, day.toPickerMillis().pickerMillisToLocalDate())
        }
    }

    @Test
    fun `el DatePicker entrega medianoche UTC y se lee como ese mismo dia`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima")) // UTC-5: leerlo en zona local daria el dia anterior
        val midnightUtc = 1_795_737_600_000L // 2026-11-27T00:00:00Z
        assertEquals(LocalDate.of(2026, 11, 27), midnightUtc.pickerMillisToLocalDate())
    }

    @Test
    fun `la fecha se muestra segun el idioma y no en formato ISO`() {
        val day = LocalDate.of(2026, 11, 27)
        val es = formatFilterDate(day, Locale.forLanguageTag("es-PE"))
        val en = formatFilterDate(day, Locale.US)
        assertNotEquals(es, en)
        assertTrue(es.contains("2026") && en.contains("2026"))
        assertNotEquals("2026-11-27", es)
    }
}

package com.jamsell.gethics.analytics.presentation

import com.jamsell.gethics.analytics.presentation.reports.isValidRange
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReportsRangeTest {

    private val day = LocalDate.of(2026, 11, 6)

    @Test
    fun `mismo dia es un rango valido`() = assertTrue(isValidRange(day, day))

    @Test
    fun `desde antes que hasta es valido`() = assertTrue(isValidRange(day.minusDays(1), day))

    @Test
    fun `desde despues de hasta es invalido`() = assertFalse(isValidRange(day.plusDays(1), day))
}

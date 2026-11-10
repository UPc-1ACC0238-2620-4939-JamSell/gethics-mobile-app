package com.jamsell.gethics.analytics.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportFileNameTest {

    private val from = "2026-11-01"
    private val to = "2026-11-30"

    @Test
    fun `toma el nombre del header Content-Disposition`() {
        val header = "attachment; filename=\"reporte-ganado-2026-11-01-2026-11-30.csv\""
        assertEquals("reporte-ganado-2026-11-01-2026-11-30.csv", exportFileName(header, from, to))
    }

    @Test
    fun `sin header arma el nombre con el rango`() {
        assertEquals("reporte-ganado-2026-11-01-2026-11-30.csv", exportFileName(null, from, to))
    }

    @Test
    fun `header sin filename usa el nombre por defecto`() {
        assertEquals("reporte-ganado-2026-11-01-2026-11-30.csv", exportFileName("attachment", from, to))
    }

    @Test
    fun `quita separadores de ruta del nombre`() {
        assertEquals("x_y.csv", exportFileName("attachment; filename=\"x/y.csv\"", from, to))
    }

    @Test
    fun `un nombre que empieza con punto se descarta`() {
        assertEquals("reporte-ganado-2026-11-01-2026-11-30.csv", exportFileName("attachment; filename=\".hidden\"", from, to))
    }
}

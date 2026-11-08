package com.jamsell.gethics.analytics.presentation.alerts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

class AlertFormatTest {

    private val lima = ZoneId.of("America/Lima")
    private val spanish = Locale.forLanguageTag("es")
    private val english = Locale.forLanguageTag("en")

    @Test
    fun `Instant UTC se muestra en la zona indicada`() {
        // Lima es UTC-5: 10:00Z -> 05:00 del mismo dia
        assertEquals("02 nov 2026, 05:00", formatAlertDate("2026-11-02T10:00:00Z", lima, spanish))
    }

    @Test
    fun `el mes abreviado sigue el idioma activo`() {
        assertEquals("02 ago 2026, 05:00", formatAlertDate("2026-08-02T10:00:00Z", lima, spanish))
        assertEquals("02 Aug 2026, 05:00", formatAlertDate("2026-08-02T10:00:00Z", lima, english))
    }

    @Test
    fun `fecha invalida o ausente devuelve null para el fallback localizado de la UI`() {
        assertNull(formatAlertDate("2026-11-02 10:00", lima, spanish))
        assertNull(formatAlertDate(null, lima, spanish))
    }
}

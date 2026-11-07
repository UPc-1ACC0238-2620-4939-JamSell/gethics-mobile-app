package com.jamsell.gethics.analytics.presentation.alerts

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class AlertFormatTest {

    @Test
    fun `Instant UTC se muestra en la zona indicada`() {
        // Lima es UTC-5: 10:00Z -> 05:00 del mismo dia
        assertEquals("02 nov 2026, 05:00", formatAlertDate("2026-11-02T10:00:00Z", ZoneId.of("America/Lima")))
    }

    @Test
    fun `fecha invalida o ausente usa el fallback`() {
        assertEquals(NO_DATE, formatAlertDate("2026-11-02 10:00", ZoneId.of("America/Lima")))
        assertEquals(NO_DATE, formatAlertDate(null, ZoneId.of("America/Lima")))
    }
}

package com.jamsell.gethics.livestock.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimalStatusTest {

    @Test
    fun `fromName reconoce los cuatro estados del backend`() {
        assertEquals(AnimalStatus.ACTIVE, AnimalStatus.fromName("ACTIVE"))
        assertEquals(AnimalStatus.SOLD, AnimalStatus.fromName("SOLD"))
        assertEquals(AnimalStatus.DECEASED, AnimalStatus.fromName("DECEASED"))
        assertEquals(AnimalStatus.INACTIVE, AnimalStatus.fromName("INACTIVE"))
    }

    @Test
    fun `fromName sin dato o desconocido devuelve null`() {
        assertNull(AnimalStatus.fromName(null))
        assertNull(AnimalStatus.fromName(""))
        assertNull(AnimalStatus.fromName("active"))
        assertNull(AnimalStatus.fromName("OTRO"))
    }
}

package com.jamsell.gethics.iot

import com.jamsell.gethics.iot.data.DeviceResponse
import com.jamsell.gethics.iot.data.ReadingsPageResponse
import com.jamsell.gethics.iot.data.isValidDeviceCode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IotModelsTest {

    private fun page(page: Int, totalPages: Int) =
        ReadingsPageResponse(content = emptyList(), page = page, size = 20, totalElements = 0, totalPages = totalPages)

    @Test
    fun `sin lecturas no hay mas paginas`() = assertFalse(page(page = 0, totalPages = 0).hasMore)

    @Test
    fun `una sola pagina no tiene mas`() = assertFalse(page(page = 0, totalPages = 1).hasMore)

    @Test
    fun `en la primera de varias paginas hay mas`() = assertTrue(page(page = 0, totalPages = 3).hasMore)

    @Test
    fun `en la ultima pagina no hay mas`() = assertFalse(page(page = 2, totalPages = 3).hasMore)

    @Test
    fun `codigo vacio o en blanco es invalido`() {
        assertFalse(isValidDeviceCode(""))
        assertFalse(isValidDeviceCode("   "))
    }

    @Test
    fun `codigo de hasta 50 caracteres es valido y de 51 no`() {
        assertTrue(isValidDeviceCode("COLLAR-001"))
        assertTrue(isValidDeviceCode("a".repeat(50)))
        assertFalse(isValidDeviceCode("a".repeat(51)))
    }

    @Test
    fun `el estado CONNECTED se reconoce como conectado`() {
        val connected = DeviceResponse("1", "o", "C-1", "CONNECTED", null, null)
        val disconnected = connected.copy(status = "DISCONNECTED")
        assertTrue(connected.isConnected)
        assertFalse(disconnected.isConnected)
    }
}

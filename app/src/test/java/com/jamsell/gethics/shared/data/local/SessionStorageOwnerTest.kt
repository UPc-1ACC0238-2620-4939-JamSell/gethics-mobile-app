package com.jamsell.gethics.shared.data.local

import com.jamsell.gethics.shared.common.Constants
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionStorageOwnerTest {

    @Test
    fun `sin login guardado usa el ownerId por defecto`() =
        assertEquals(Constants.DEFAULT_OWNER_ID, SessionStorage(FakeSharedPreferences()).getOwnerId())

    @Test
    fun `devuelve el ownerId guardado`() {
        val session = SessionStorage(FakeSharedPreferences())
        session.saveOwnerId("11111111-1111-1111-1111-111111111111")
        assertEquals("11111111-1111-1111-1111-111111111111", session.getOwnerId())
    }
}

package com.jamsell.gethics.sanitary.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.shared.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** US35: el DAO real de Room (en memoria; no toca la base de la app instalada). */
@RunWith(AndroidJUnit4::class)
class PendingSanitaryOperationDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: PendingSanitaryOperationDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.getPendingSanitaryOperationDao()
    }

    @After
    fun tearDown() = database.close()

    private fun operation(id: String, createdAt: Long, type: PendingSanitaryOperationType = PendingSanitaryOperationType.REGISTER) =
        PendingSanitaryOperationEntity(id, type, "animal-uuid", SanitaryEventType.VACCINATION, "2026-11-20", "Aftosa", createdAt)

    @Test
    fun insertarYRecuperarPendientesConTodosSusCampos() = runBlocking {
        val register = operation("a", 1)
        val schedule = operation("b", 2, PendingSanitaryOperationType.SCHEDULE).copy(description = null)
        dao.insert(register)
        dao.insert(schedule)

        assertEquals(listOf(register, schedule), dao.getPending()) // enums y nulls ida y vuelta
    }

    @Test
    fun devuelveLosPendientesEnOrdenFifo() = runBlocking {
        dao.insert(operation("tercera", 30))
        dao.insert(operation("primera", 10))
        dao.insert(operation("segunda", 20))
        dao.insert(operation("empate", 20)) // mismo milisegundo: decide el orden de insercion (rowid)

        assertEquals(listOf("primera", "segunda", "empate", "tercera"), dao.getPending().map { it.clientRequestId })
    }

    @Test
    fun eliminaPorClientRequestId() = runBlocking {
        dao.insert(operation("a", 1))
        dao.insert(operation("b", 2))

        dao.delete("a")

        assertEquals(listOf("b"), dao.getPending().map { it.clientRequestId })
    }

    @Test
    fun markFailedLaSacaDePendientesYGuardaElError() = runBlocking {
        dao.insert(operation("a", 1))
        dao.insert(operation("b", 2))

        dao.markFailed("a", "El clientRequestId ya se uso con datos distintos.")

        assertEquals(listOf("b"), dao.getPending().map { it.clientRequestId })
        val cursor = database.query("select status, last_error from pending_sanitary_operations where client_request_id = 'a'", null)
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("FAILED", it.getString(0))
            assertEquals("El clientRequestId ya se uso con datos distintos.", it.getString(1))
        }
    }

    @Test
    fun insertarDosVecesElMismoClientRequestIdFalla() {
        runBlocking { dao.insert(operation("a", 1)) }

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { dao.insert(operation("a", 2)) }
        }
        runBlocking { assertEquals(1, dao.getPending().size) }
    }
}

package com.jamsell.gethics.shared.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jamsell.gethics.livestock.data.local.AnimalDao
import com.jamsell.gethics.livestock.data.local.AnimalEntity
import com.jamsell.gethics.sanitary.data.local.PendingSanitaryOperationDao
import com.jamsell.gethics.sanitary.data.local.PendingSanitaryOperationEntity

/**
 * Una sola base Room para toda la app (modo offline).
 * Cada bounded context define sus Entity/Dao en su propia carpeta data/local
 * y SOLO se registran aqui. Si agregas una Entity, sube `version`.
 *
 * IMPORTANTE (desde la version 5, US35): pending_sanitary_operations NO es cache, guarda operaciones del usuario que
 * aun no llegaron al backend. AppContainer usa fallbackToDestructiveMigration, que al cambiar de version BORRA todas
 * las tablas: el paso 4 -> 5 es seguro porque la cola aun no existia, pero cualquier version futura debe registrar una
 * Migration explicita (addMigrations) que conserve pending_sanitary_operations, o se perderan operaciones sin enviar.
 */
@Database(
    entities = [
        AnimalEntity::class,
        PendingSanitaryOperationEntity::class
        // TODO: TransactionEntity (finance)...
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getAnimalDao(): AnimalDao

    abstract fun getPendingSanitaryOperationDao(): PendingSanitaryOperationDao
}

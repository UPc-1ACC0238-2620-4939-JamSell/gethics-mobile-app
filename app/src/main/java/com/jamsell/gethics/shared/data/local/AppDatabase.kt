package com.jamsell.gethics.shared.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jamsell.gethics.livestock.data.local.AnimalDao
import com.jamsell.gethics.livestock.data.local.AnimalEntity

/**
 * Una sola base Room para toda la app (modo offline).
 * Cada bounded context define sus Entity/Dao en su propia carpeta data/local
 * y SOLO se registran aqui. Si agregas una Entity, sube `version`.
 */
@Database(
    entities = [
        AnimalEntity::class
        // TODO: SanitaryEventEntity (sanitary), TransactionEntity (finance)...
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getAnimalDao(): AnimalDao
}

package com.jamsell.gethics.sanitary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** US35: cola offline de sanidad (ver PendingSanitaryOperationEntity). */
@Dao
interface PendingSanitaryOperationDao {
    /** ABORT: un clientRequestId nunca se repite; si se repitiera es un error, no algo que reemplazar. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(operation: PendingSanitaryOperationEntity)

    /** FIFO; rowid desempata operaciones creadas en el mismo milisegundo. */
    @Query("select * from pending_sanitary_operations where status = 'PENDING' order by created_at, rowid")
    suspend fun getPending(): List<PendingSanitaryOperationEntity>

    @Query("delete from pending_sanitary_operations where client_request_id = :clientRequestId")
    suspend fun delete(clientRequestId: String)

    /** Rechazo permanente del backend: se conserva para no perder el dato en silencio y no se vuelve a enviar. */
    @Query("update pending_sanitary_operations set status = 'FAILED', last_error = :message where client_request_id = :clientRequestId")
    suspend fun markFailed(clientRequestId: String, message: String)
}

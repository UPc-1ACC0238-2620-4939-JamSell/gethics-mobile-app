package com.jamsell.gethics.sanitary.data.sync

import com.jamsell.gethics.sanitary.data.SanitaryService
import com.jamsell.gethics.sanitary.data.local.PendingSanitaryOperationDao
import com.jamsell.gethics.sanitary.data.local.PendingSanitaryOperationEntity
import com.jamsell.gethics.sanitary.data.local.PendingSanitaryOperationType
import com.jamsell.gethics.sanitary.data.local.toRegisterRequest
import com.jamsell.gethics.sanitary.data.local.toScheduleRequest
import com.jamsell.gethics.shared.data.remote.errorMessage
import retrofit2.Response
import java.io.IOException

/** SUCCESS: no queda nada que reintentar (todo enviado o solo FAILED). RETRY: hay que volver a intentarlo mas tarde. */
enum class SyncResult { SUCCESS, RETRY }

/**
 * US35: envia la cola offline de sanidad, sin dependencias de Android (SanitarySyncWorker solo lo invoca).
 * Reenvia cada operacion con SU clientRequestId guardado, nunca uno nuevo: el backend responde 200 con el evento existente
 * si un intento anterior ya habia llegado.
 * - 2xx (201 creado, 200 duplicado): se borra la fila y sigue con la siguiente.
 * - IOException, 408, 429, 5xx: la fila queda igual y se corta con RETRY (sin red o backend caido: el resto fallaria igual).
 * - Otro 4xx (400, 404, 409...): FAILED con el message del backend; sigue con la siguiente, una invalida no bloquea la cola.
 */
class SanitarySyncer(
    private val service: SanitaryService,
    private val dao: PendingSanitaryOperationDao
) {
    suspend fun syncAll(): SyncResult {
        for (operation in dao.getPending()) { // FIFO
            val response = try {
                send(operation)
            } catch (e: IOException) {
                return SyncResult.RETRY
            }
            when {
                response.isSuccessful -> dao.delete(operation.clientRequestId)
                isRetryable(response.code()) -> return SyncResult.RETRY
                else -> dao.markFailed(operation.clientRequestId, response.errorMessage())
            }
        }
        return SyncResult.SUCCESS
    }

    private suspend fun send(operation: PendingSanitaryOperationEntity): Response<Unit> = when (operation.operation) {
        PendingSanitaryOperationType.REGISTER -> service.registerEvent(operation.animalId, operation.toRegisterRequest())
        PendingSanitaryOperationType.SCHEDULE -> service.scheduleEvent(operation.animalId, operation.toScheduleRequest())
    }

    private fun isRetryable(code: Int) = code == 408 || code == 429 || code >= 500
}

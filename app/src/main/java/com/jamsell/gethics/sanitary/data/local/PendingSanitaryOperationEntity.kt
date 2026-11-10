package com.jamsell.gethics.sanitary.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jamsell.gethics.sanitary.data.RegisterSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.sanitary.data.ScheduleSanitaryEventRequest

/** US35: operaciones que se pueden crear sin conexion. Room guarda los enums por nombre. */
enum class PendingSanitaryOperationType { REGISTER, SCHEDULE }

/** Sin SYNCING (quedaria colgado si Android mata el proceso) ni SYNCED (tras el exito la fila se borra). */
enum class PendingSanitaryOperationStatus { PENDING, FAILED }

/**
 * US35: operacion de sanidad guardada sin conexion, a la espera de SanitarySyncWorker. NO es cache: son datos del
 * usuario que aun no llegaron al backend. clientRequestId es la clave y se reenvia igual en cada reintento.
 * date = occurredAt (REGISTER) o scheduledDate (SCHEDULE), ya en el formato ISO del request.
 */
@Entity(tableName = "pending_sanitary_operations")
data class PendingSanitaryOperationEntity(
    @PrimaryKey
    @ColumnInfo("client_request_id")
    val clientRequestId: String,
    @ColumnInfo("operation")
    val operation: PendingSanitaryOperationType,
    @ColumnInfo("animal_id")
    val animalId: String,
    @ColumnInfo("event_type")
    val eventType: SanitaryEventType,
    @ColumnInfo("date")
    val date: String,
    @ColumnInfo("description")
    val description: String?,
    @ColumnInfo("created_at")
    val createdAt: Long,
    @ColumnInfo("status")
    val status: PendingSanitaryOperationStatus = PendingSanitaryOperationStatus.PENDING,
    @ColumnInfo("last_error")
    val lastError: String? = null
)

fun RegisterSanitaryEventRequest.toPending(animalId: String, createdAt: Long) = PendingSanitaryOperationEntity(
    clientRequestId, PendingSanitaryOperationType.REGISTER, animalId, type, occurredAt, description, createdAt
)

fun ScheduleSanitaryEventRequest.toPending(animalId: String, createdAt: Long) = PendingSanitaryOperationEntity(
    clientRequestId, PendingSanitaryOperationType.SCHEDULE, animalId, type, scheduledDate, description, createdAt
)

fun PendingSanitaryOperationEntity.toRegisterRequest() = RegisterSanitaryEventRequest(eventType, date, description, clientRequestId)

fun PendingSanitaryOperationEntity.toScheduleRequest() = ScheduleSanitaryEventRequest(eventType, date, description, clientRequestId)

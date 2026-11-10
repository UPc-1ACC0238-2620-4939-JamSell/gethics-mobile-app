package com.jamsell.gethics.sanitary.data.repository

import com.jamsell.gethics.sanitary.data.ClinicalHistoryResponse
import com.jamsell.gethics.sanitary.data.CompleteSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.ScheduleSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.RegisterSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryCalendarResponse
import com.jamsell.gethics.sanitary.data.SanitaryEventType
import com.jamsell.gethics.sanitary.data.SanitaryService
import com.jamsell.gethics.sanitary.data.local.PendingSanitaryOperationDao
import com.jamsell.gethics.sanitary.data.local.PendingSanitaryOperationEntity
import com.jamsell.gethics.sanitary.data.local.toPending
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.remote.errorMessage
import retrofit2.Response
import java.io.IOException
import java.time.YearMonth
import java.util.UUID

const val CONNECTION_ERROR = "No se pudo conectar con el servidor"

/** US35: SENT = el backend lo guardo; QUEUED = sin conexion, quedo en Room y se enviara cuando vuelva la red. */
enum class SanitarySaveResult { SENT, QUEUED }

/**
 * pendingDao / onQueued (US35): cola offline para registrar y programar eventos. onQueued avisa que hay algo que
 * sincronizar (AppContainer programa WorkManager), asi el repository no depende de Android. Sin pendingDao (tests de
 * otras pantallas) no hay cola y sin conexion se devuelve el error de conexion, como antes de US35.
 */
class SanitaryRepository(
    private val service: SanitaryService,
    private val pendingDao: PendingSanitaryOperationDao? = null,
    private val onQueued: () -> Unit = {}
) {

    /**
     * US11 + US35. El clientRequestId se genera UNA vez aqui, antes del primer intento: si el POST llego al backend pero
     * la respuesta se perdio (IOException), el reintento con el mismo id devuelve el evento ya creado en vez de duplicarlo.
     */
    suspend fun registerEvent(
        animalId: String,
        type: SanitaryEventType,
        occurredAt: String,
        description: String?
    ): Resource<SanitarySaveResult> {
        val request = RegisterSanitaryEventRequest(type, occurredAt, description, newClientRequestId())
        return sendOrQueue({ service.registerEvent(animalId, request) }) { request.toPending(animalId, it) }
    }

    suspend fun getCalendar(period: YearMonth): Resource<SanitaryCalendarResponse> =
        try {
            val response = service.getCalendar(period.year, period.monthValue)
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    /** US13 + US35: programa un evento (la pantalla solo envia VACCINATION); mismo manejo offline que registerEvent. */
    suspend fun scheduleEvent(
        animalId: String,
        type: SanitaryEventType,
        scheduledDate: String,
        description: String?
    ): Resource<SanitarySaveResult> {
        val request = ScheduleSanitaryEventRequest(type, scheduledDate, description, newClientRequestId())
        return sendOrQueue({ service.scheduleEvent(animalId, request) }) { request.toPending(animalId, it) }
    }

    /** Un error HTTP (400, 409...) NO se encola: es la respuesta del backend y la pantalla la muestra. */
    private suspend fun sendOrQueue(
        send: suspend () -> Response<Unit>,
        pending: (createdAt: Long) -> PendingSanitaryOperationEntity
    ): Resource<SanitarySaveResult> =
        try {
            val response = send()
            if (response.isSuccessful) Resource.Success(SanitarySaveResult.SENT) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            val dao = pendingDao
            if (dao == null) {
                Resource.Error(CONNECTION_ERROR)
            } else {
                dao.insert(pending(System.currentTimeMillis())) // mismo clientRequestId que el intento fallido
                onQueued()
                Resource.Success(SanitarySaveResult.QUEUED)
            }
        }

    private fun newClientRequestId() = UUID.randomUUID().toString()

    /** US13: registra como aplicado un evento programado. */
    suspend fun completeEvent(animalId: String, eventId: String, request: CompleteSanitaryEventRequest): Resource<Unit> =
        try {
            val response = service.completeEvent(animalId, eventId, request)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    /** US33: cancela un evento programado. */
    suspend fun cancelEvent(animalId: String, eventId: String): Resource<Unit> =
        try {
            val response = service.cancelEvent(animalId, eventId)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    suspend fun getClinicalHistory(animalId: String): Resource<ClinicalHistoryResponse> =
        try {
            val response = service.getClinicalHistory(animalId)
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }
}

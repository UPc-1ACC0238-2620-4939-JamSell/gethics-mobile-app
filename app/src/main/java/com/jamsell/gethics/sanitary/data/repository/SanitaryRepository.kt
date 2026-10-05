package com.jamsell.gethics.sanitary.data.repository

import com.jamsell.gethics.sanitary.data.ClinicalHistoryResponse
import com.jamsell.gethics.sanitary.data.RegisterSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryCalendarResponse
import com.jamsell.gethics.sanitary.data.SanitaryService
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.remote.errorMessage
import java.io.IOException
import java.time.YearMonth

const val CONNECTION_ERROR = "No se pudo conectar con el servidor"

class SanitaryRepository(private val service: SanitaryService) {

    suspend fun registerEvent(animalId: String, request: RegisterSanitaryEventRequest): Resource<Unit> =
        try {
            val response = service.registerEvent(animalId, request)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    suspend fun getCalendar(period: YearMonth): Resource<SanitaryCalendarResponse> =
        try {
            val response = service.getCalendar(period.year, period.monthValue)
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body) else Resource.Error(response.errorMessage())
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

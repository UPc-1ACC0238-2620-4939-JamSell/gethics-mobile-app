package com.jamsell.gethics.analytics.data.repository

import com.jamsell.gethics.analytics.data.AnalyticsService
import com.jamsell.gethics.analytics.data.ExportedReport
import com.jamsell.gethics.analytics.data.LivestockReportResponse
import com.jamsell.gethics.analytics.data.exportFileName
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.remote.errorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate

private const val CONNECTION_ERROR = "No se pudo conectar con el servidor"

class AnalyticsRepository(private val service: AnalyticsService) {

    /** US20. Reporte y estadisticas del rango [from, to]. */
    suspend fun getReport(ownerId: String, from: LocalDate, to: LocalDate): Resource<LivestockReportResponse> =
        try {
            val response = service.getReport(ownerId, from.toString(), to.toString())
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    /** US39. Descarga el reporte del rango en CSV. 404 con message cuando no hay informacion en el periodo. */
    suspend fun exportReport(ownerId: String, from: LocalDate, to: LocalDate): Resource<ExportedReport> =
        try {
            val response = service.exportReport(ownerId, from.toString(), to.toString())
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val name = exportFileName(response.headers()["Content-Disposition"], from.toString(), to.toString())
                // @Streaming: el cuerpo se lee aqui, asi que debe hacerse fuera del hilo principal
                val bytes = withContext(Dispatchers.IO) { body.bytes() }
                Resource.Success(ExportedReport(name, bytes))
            } else {
                Resource.Error(response.errorMessage())
            }
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }
}

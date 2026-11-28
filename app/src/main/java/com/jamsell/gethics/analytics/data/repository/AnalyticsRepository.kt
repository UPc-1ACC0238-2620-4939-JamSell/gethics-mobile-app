package com.jamsell.gethics.analytics.data.repository

import com.jamsell.gethics.analytics.data.AnalyticsService
import com.jamsell.gethics.analytics.data.ExportedReport
import com.jamsell.gethics.analytics.data.LivestockReportResponse
import com.jamsell.gethics.analytics.data.exportFileName
import com.jamsell.gethics.analytics.data.normalizeCategory
import com.jamsell.gethics.finance.data.FinancialTransactionType
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.remote.errorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate

private const val CONNECTION_ERROR = "No se pudo conectar con el servidor"

class AnalyticsRepository(private val service: AnalyticsService) {

    /**
     * US20 + US49. Reporte y estadisticas del rango [from, to]. type y category son opcionales: null (o una categoria
     * vacia) significa "sin filtro" y el parametro no se envia. El filtrado lo hace el backend.
     */
    suspend fun getReport(
        ownerId: String,
        from: LocalDate,
        to: LocalDate,
        type: FinancialTransactionType? = null,
        category: String? = null
    ): Resource<LivestockReportResponse> =
        try {
            val response = service.getReport(ownerId, from.toString(), to.toString(), type?.name, normalizeCategory(category))
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    /**
     * US39 + US49. Descarga el reporte en CSV con los mismos parametros que getReport. 404 con message cuando no hay
     * informacion para esos filtros. El CSV llega tal cual lo genera el backend.
     */
    suspend fun exportReport(
        ownerId: String,
        from: LocalDate,
        to: LocalDate,
        type: FinancialTransactionType? = null,
        category: String? = null
    ): Resource<ExportedReport> =
        try {
            val response = service.exportReport(ownerId, from.toString(), to.toString(), type?.name, normalizeCategory(category))
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

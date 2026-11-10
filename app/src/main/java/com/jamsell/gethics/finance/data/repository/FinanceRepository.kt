package com.jamsell.gethics.finance.data.repository

import com.jamsell.gethics.finance.data.FinanceService
import com.jamsell.gethics.finance.data.FinancialSummaryResponse
import com.jamsell.gethics.finance.data.RegisterTransactionRequest
import com.jamsell.gethics.finance.data.TransactionResponse
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.remote.errorMessage
import java.io.IOException
import java.time.LocalDate

private const val CONNECTION_ERROR = "No se pudo conectar con el servidor"

class FinanceRepository(private val service: FinanceService) {

    /** US16. Resumen del periodo [from, to]; con ambos en null trae todo el historial. */
    suspend fun getSummary(ownerId: String, from: LocalDate?, to: LocalDate?): Resource<FinancialSummaryResponse> =
        try {
            val response = service.getSummary(ownerId, from?.toString(), to?.toString())
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    /** Registra un ingreso o gasto. Sin conexion devuelve error (no hay cola de sincronizacion). */
    suspend fun registerTransaction(request: RegisterTransactionRequest): Resource<TransactionResponse> =
        try {
            val response = service.registerTransaction(request)
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    /** US31. Anula un movimiento financiero por su ID. */
    suspend fun voidFinancialMovement(movementId: Long): Resource<Unit> =
        try {
            val response = service.voidFinancialMovement(movementId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error(response.errorMessage())
            }
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }
}
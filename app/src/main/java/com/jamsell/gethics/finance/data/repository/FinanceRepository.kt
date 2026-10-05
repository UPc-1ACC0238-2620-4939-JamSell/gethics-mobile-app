package com.jamsell.gethics.finance.data.repository

import com.jamsell.gethics.finance.data.FinanceService
import com.jamsell.gethics.finance.data.FinancialSummaryResponse
import com.jamsell.gethics.finance.data.RegisterTransactionRequest
import com.jamsell.gethics.finance.data.TransactionDto
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.remote.errorMessage
import java.io.IOException

private const val CONNECTION_ERROR = "No se pudo conectar con el servidor"

class FinanceRepository(private val service: FinanceService) {

    suspend fun registerTransaction(request: RegisterTransactionRequest): Resource<TransactionDto> =
        try {
            val response = service.registerTransaction(request)
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body)
            else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    suspend fun getSummary(ownerId: String): Resource<FinancialSummaryResponse> =
        try {
            val response = service.getSummary(ownerId)
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body)
            else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }
}
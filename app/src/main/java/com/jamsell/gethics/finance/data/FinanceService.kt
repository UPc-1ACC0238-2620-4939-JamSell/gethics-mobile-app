package com.jamsell.gethics.finance.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface FinanceService {

    @POST("api/v1/finances")
    suspend fun registerTransaction(@Body request: RegisterTransactionRequest): Response<TransactionDto>

    @GET("api/v1/finances")
    suspend fun getSummary(@Query("ownerId") ownerId: String): Response<FinancialSummaryResponse>
}
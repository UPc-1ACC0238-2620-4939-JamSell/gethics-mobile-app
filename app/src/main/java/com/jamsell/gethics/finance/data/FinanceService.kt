package com.jamsell.gethics.finance.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface FinanceService {

    /** Registra un ingreso o gasto. 201 con el movimiento; 400 si el monto es <= 0 o falta un campo. */
    @POST("api/v1/finances")
    suspend fun registerTransaction(@Body request: RegisterTransactionRequest): Response<TransactionResponse>

    /**
     * US16. Balance y movimientos del ganadero. from y to (yyyy-MM-dd) van juntos o ninguno:
     * si falta uno el backend responde 400. Sin ellos devuelve todo el historial.
     */
    @GET("api/v1/finances")
    suspend fun getSummary(
        @Query("ownerId") ownerId: String,
        @Query("from") from: String?,
        @Query("to") to: String?
    ): Response<FinancialSummaryResponse>
}

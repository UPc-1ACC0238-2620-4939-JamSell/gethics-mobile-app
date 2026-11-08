package com.jamsell.gethics.finance.data

/** Tipos que acepta el backend. Gson los envia por nombre (INCOME / EXPENSE). */
enum class FinancialTransactionType { INCOME, EXPENSE }

/** Body de RegisterTransactionResource (POST api/v1/finances). date: yyyy-MM-dd. description es opcional. */
data class RegisterTransactionRequest(
    val ownerId: String,
    val type: FinancialTransactionType,
    val amount: Double,
    val category: String,
    val date: String,
    val description: String?
)

/** TransactionResource: un movimiento del ganadero. */
data class TransactionResponse(
    val id: String,
    val type: FinancialTransactionType,
    val amount: Double,
    val category: String,
    val date: String,
    val description: String?
)

/**
 * FinancialSummaryResource (US16). balance = ingresos - gastos.
 * message viene solo cuando el periodo no tiene movimientos ("No existen movimientos...").
 */
data class FinancialSummaryResponse(
    val ownerId: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val transactions: List<TransactionResponse>,
    val message: String?
)

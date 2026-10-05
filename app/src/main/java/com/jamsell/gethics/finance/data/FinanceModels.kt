package com.jamsell.gethics.finance.data

enum class TransactionType(val label: String) {
    INCOME("Ingreso"),
    EXPENSE("Gasto")
}

/** RegisterTransactionResource del backend. date en formato yyyy-MM-dd. */
data class RegisterTransactionRequest(
    val ownerId: String,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val date: String,
    val description: String?
)

/** TransactionResource del backend. */
data class TransactionDto(
    val id: String,
    val type: TransactionType?,
    val amount: Double,
    val category: String,
    val date: String,
    val description: String?
)

/** FinancialSummaryResource del backend: balance actual y movimientos. */
data class FinancialSummaryResponse(
    val ownerId: String,
    val balance: Double,
    val transactions: List<TransactionDto>
)
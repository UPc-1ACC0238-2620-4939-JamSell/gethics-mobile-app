package com.jamsell.gethics.analytics.data

/** Seccion de salud o productividad del reporte. limitedData = true cuando hay pocos datos; message lo explica. */
data class ReportSectionResponse(
    val limitedData: Boolean,
    val message: String?
)

/** Seccion financiera del reporte: totales del rango. */
data class FinanceReportSectionResponse(
    val totalIncome: Double,
    val totalExpense: Double,
    val netResult: Double,
    val limitedData: Boolean,
    val message: String?
)

/** LivestockReportResource (US20). from y to: yyyy-MM-dd. */
data class LivestockReportResponse(
    val ownerId: String,
    val from: String,
    val to: String,
    val finance: FinanceReportSectionResponse,
    val health: ReportSectionResponse,
    val productivity: ReportSectionResponse
)

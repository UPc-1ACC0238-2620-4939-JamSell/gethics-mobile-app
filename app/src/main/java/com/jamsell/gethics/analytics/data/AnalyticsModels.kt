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

/** US39. Archivo CSV descargado del backend, listo para guardarse y compartirse. */
class ExportedReport(val fileName: String, val bytes: ByteArray)

/**
 * Nombre del archivo a partir del header Content-Disposition (filename="..."). Si no viene o es invalido,
 * se arma uno con el rango. Se quitan separadores de ruta para no escribir fuera de la carpeta de cache.
 */
fun exportFileName(contentDisposition: String?, from: String, to: String): String {
    val fallback = "reporte-ganado-$from-$to.csv"
    val raw = contentDisposition
        ?.let { Regex("filename=\"?([^\";]+)\"?").find(it)?.groupValues?.get(1) }
        ?.trim()
        ?: return fallback
    val clean = raw.replace(Regex("[\\\\/]"), "_")
    return clean.takeIf { it.isNotBlank() && !it.startsWith(".") } ?: fallback
}

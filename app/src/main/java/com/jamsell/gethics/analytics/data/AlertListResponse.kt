package com.jamsell.gethics.analytics.data

/** Valores del backend (analytics). Gson los lee por nombre; un valor desconocido queda en null. */
enum class TrendType(val label: String) {
    SANITARY("Sanitaria"),
    FINANCIAL("Financiera"),
    COMBINED("Combinada")
}

enum class RiskLevel(val label: String) {
    LOW("Bajo"),
    MEDIUM("Medio"),
    HIGH("Alto"),
    CRITICAL("Crítico")
}

enum class AlertStatus(val label: String) {
    PENDING("Pendiente"),
    SENT("Enviada"),
    READ("Leída")
}

/** AlertListResource (US21). message solo trae valor cuando alerts esta vacio ("No hay alertas registradas."). */
data class AlertListResponse(
    val alerts: List<AlertDto>,
    val message: String?
)

/** AlertResource. Llegan ordenadas por createdAt descendente (lo garantiza el backend). */
data class AlertDto(
    val id: String,              // UUID: solo como key de la lista, no se muestra
    val trendId: String,         // UUID: no se muestra
    val trendType: TrendType?,
    val riskLevel: RiskLevel?,   // riesgo de la tendencia que origino la alerta
    val message: String,
    val status: AlertStatus?,
    val createdAt: String        // Instant ISO-8601 en UTC (ej. 2026-11-01T10:00:00Z)
)

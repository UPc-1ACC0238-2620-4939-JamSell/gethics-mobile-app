package com.jamsell.gethics.analytics.presentation.alerts

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Locale

/** Mismo texto que US14 para una fecha que no se puede leer: no se inventa ninguna. */
const val NO_DATE = "Sin fecha registrada"

// Abreviaturas fijas, como en US14: iguales en cualquier dispositivo ("02 nov 2026").
private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/**
 * createdAt (Instant ISO en UTC) en la zona del dispositivo: "02 nov 2026, 05:00".
 * [zone] se inyecta para poder probarlo; la pantalla usa ZoneId.systemDefault().
 */
fun formatAlertDate(createdAt: String?, zone: ZoneId): String {
    val instant = createdAt?.let {
        try {
            Instant.parse(it)
        } catch (e: DateTimeParseException) {
            null
        }
    } ?: return NO_DATE
    val local = instant.atZone(zone)
    return String.format(
        Locale.ROOT, "%02d %s %d, %02d:%02d",
        local.dayOfMonth, MONTHS[local.monthValue - 1], local.year, local.hour, local.minute
    )
}

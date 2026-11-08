package com.jamsell.gethics.analytics.presentation.alerts

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

// Mes abreviado segun el idioma activo: "02 ago 2026, 05:00" (es) / "02 Aug 2026, 05:00" (en).
private const val DATE_PATTERN = "dd MMM yyyy, HH:mm"

/**
 * createdAt (Instant ISO en UTC) en la zona y el idioma del dispositivo.
 * Devuelve null si la fecha no se puede leer (no se inventa ninguna): la pantalla muestra alerts_no_date.
 * [zone] y [locale] se inyectan para poder probarlo; la pantalla usa ZoneId.systemDefault() y el locale activo.
 */
fun formatAlertDate(createdAt: String?, zone: ZoneId, locale: Locale): String? {
    val instant = createdAt?.let {
        try {
            Instant.parse(it)
        } catch (e: DateTimeParseException) {
            null
        }
    } ?: return null
    return DateTimeFormatter.ofPattern(DATE_PATTERN, locale).format(instant.atZone(zone))
}

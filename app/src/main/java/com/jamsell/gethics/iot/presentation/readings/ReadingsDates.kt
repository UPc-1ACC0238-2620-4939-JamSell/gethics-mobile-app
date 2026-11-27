package com.jamsell.gethics.iot.presentation.readings

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// El DatePicker de Material 3 trabaja con la medianoche UTC del dia elegido; leerlo en la zona local desplaza un dia.
internal fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun Long.pickerMillisToLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Fecha para mostrar en pantalla segun el idioma actual (al backend siempre se envia yyyy-MM-dd). */
internal fun formatFilterDate(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

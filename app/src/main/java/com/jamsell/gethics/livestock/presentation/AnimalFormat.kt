package com.jamsell.gethics.livestock.presentation

import com.jamsell.gethics.livestock.domain.model.AnimalSex
import java.time.LocalDate
import java.time.Period

/** Texto que se muestra cuando falta un dato; no se inventa ninguno. */
const val NO_DATA = "—"
const val NO_SEX = "Sin especificar"

fun sexLabel(sex: String?): String = AnimalSex.entries.firstOrNull { it.name == sex }?.label ?: NO_SEX

private fun parseDate(iso: String?): LocalDate? = iso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/** "12/05/2022" a partir de la fecha ISO del backend. */
fun formatBirthDate(iso: String?): String =
    parseDate(iso)?.let { "%02d/%02d/%d".format(it.dayOfMonth, it.monthValue, it.year) } ?: NO_DATA

/** "4 años", "8 meses" o "Menos de 1 mes". {@code today} se inyecta para poder probarlo. */
fun formatAge(birthDateIso: String?, today: LocalDate): String {
    val birth = parseDate(birthDateIso)?.takeIf { !it.isAfter(today) } ?: return NO_DATA
    val period = Period.between(birth, today)
    return when {
        period.years == 1 -> "1 año"
        period.years > 1 -> "${period.years} años"
        period.months == 1 -> "1 mes"
        period.months > 1 -> "${period.months} meses"
        else -> "Menos de 1 mes"
    }
}

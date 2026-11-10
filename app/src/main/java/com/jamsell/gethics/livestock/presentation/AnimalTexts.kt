package com.jamsell.gethics.livestock.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.jamsell.gethics.R
import com.jamsell.gethics.livestock.domain.model.Animal
import com.jamsell.gethics.livestock.presentation.animal_register.FUTURE_BIRTH_DATE_ERROR
import com.jamsell.gethics.livestock.presentation.animal_register.INVALID_WEIGHT_ERROR
import com.jamsell.gethics.livestock.presentation.animal_register.REQUIRED_FIELD_ERROR
import java.time.LocalDate
import java.time.Period

/**
 * Textos del animal en el idioma de la app (res/values y res/values-en). Las funciones puras de AnimalFormat.kt
 * (formatAge, sexLabel, summaryLine) siguen en espanol para las pruebas unitarias; las pantallas usan estas.
 */

/** "Macho" / "Male", "Hembra" / "Female"; sin dato o desconocido, "Sin especificar" / "Unspecified". */
@Composable
fun sexText(sex: String?): String = when (sex) {
    "MALE" -> stringResource(R.string.animal_sex_male)
    "FEMALE" -> stringResource(R.string.animal_sex_female)
    else -> stringResource(R.string.animal_sex_unspecified)
}

/** Misma regla que formatAge, con plurales del idioma de la app. Sin fecha valida: {@link NO_DATA}. */
@Composable
fun ageText(birthDateIso: String?, today: LocalDate): String {
    val birth = runCatching { LocalDate.parse(birthDateIso) }.getOrNull()?.takeIf { !it.isAfter(today) } ?: return NO_DATA
    val period = Period.between(birth, today)
    return when {
        period.years >= 1 -> pluralStringResource(R.plurals.animal_age_years, period.years, period.years)
        period.months >= 1 -> pluralStringResource(R.plurals.animal_age_months, period.months, period.months)
        else -> stringResource(R.string.animal_age_less_than_month)
    }
}

/** "HOLSTEIN · HEMBRA · 4 AÑOS" en el idioma de la app: solo lo que se conoce. */
@Composable
fun summaryText(animal: Animal, today: LocalDate): String {
    val sex = animal.sex?.let { sexText(it) }
    val age = ageText(animal.birthDate, today).takeIf { it != NO_DATA }
    return listOfNotNull(animal.breed, sex, age).joinToString(" · ").uppercase()
}

/**
 * Mensajes de validacion de los formularios en el idioma de la app. Los ViewModels guardan los textos en espanol
 * (constantes que prueban los tests); aqui se traducen. Un mensaje que no sea uno de ellos (por ejemplo, el que envia
 * el backend) se muestra tal cual.
 */
@Composable
fun localizedAnimalError(error: String?): String? = when (error) {
    null -> null
    REQUIRED_FIELD_ERROR -> stringResource(R.string.animal_error_required)
    FUTURE_BIRTH_DATE_ERROR -> stringResource(R.string.animal_error_future_birth)
    INVALID_WEIGHT_ERROR -> stringResource(R.string.animal_error_invalid_weight)
    else -> error
}

/** US-36. Aviso de modo sin conexion; con fecha de ultima actualizacion si se conoce. */
@Composable
fun offlineBannerText(lastUpdatedMillis: Long?): String {
    val date = formatLastUpdate(lastUpdatedMillis) ?: return stringResource(R.string.livestock_offline)
    return stringResource(R.string.livestock_offline_since, date)
}

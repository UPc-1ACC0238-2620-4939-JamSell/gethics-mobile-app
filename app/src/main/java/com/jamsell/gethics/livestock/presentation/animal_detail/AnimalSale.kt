package com.jamsell.gethics.livestock.presentation.animal_detail

import java.time.LocalDate

/** Mensajes de validacion de la venta (US-30). La pantalla los traduce al idioma de la app; los tests los comparan tal cual. */
const val INVALID_PRICE_ERROR = "El precio debe ser mayor a 0"
const val INVALID_SALE_DATE_ERROR = "Ingresa una fecha válida (aaaa-mm-dd)"
const val FUTURE_SALE_DATE_ERROR = "La fecha de venta no puede ser futura"

private const val MAX_PRICE = 9_999_999_999.99

data class SaleErrors(val price: String? = null, val date: String? = null) {
    val hasErrors get() = price != null || date != null
}

/** Precio con coma o punto, mayor a 0, hasta 2 decimales y 10 enteros (igual que el backend). */
fun parsePrice(text: String): Double? {
    val normalized = text.trim().replace(',', '.')
    if (!Regex("""\d+(\.\d{1,2})?""").matches(normalized)) return null
    val value = normalized.toDoubleOrNull() ?: return null
    return value.takeIf { it > 0 && it <= MAX_PRICE }
}

fun parseSaleDate(text: String): LocalDate? = runCatching { LocalDate.parse(text.trim()) }.getOrNull()

fun validateSale(priceText: String, dateText: String, today: LocalDate = LocalDate.now()): SaleErrors {
    val date = parseSaleDate(dateText)
    return SaleErrors(
        price = if (parsePrice(priceText) == null) INVALID_PRICE_ERROR else null,
        date = when {
            date == null -> INVALID_SALE_DATE_ERROR
            date.isAfter(today) -> FUTURE_SALE_DATE_ERROR
            else -> null
        }
    )
}

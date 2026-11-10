package com.jamsell.gethics.livestock.data

/** US-30. ownerId solo hace falta si el animal no tiene granja (el backend toma el dueno de la granja). */
data class SellAnimalRequest(
    val price: Double,
    val saleDate: String,   // LocalDate ISO yyyy-MM-dd
    val ownerId: String? = null
)

/** AnimalSaleResource: el animal ya SOLD, el precio, la fecha y el id del ingreso creado en Finanzas. */
data class AnimalSaleResponse(
    val animalId: String,
    val status: String,
    val price: Double,
    val saleDate: String,
    val incomeTransactionId: String?
)

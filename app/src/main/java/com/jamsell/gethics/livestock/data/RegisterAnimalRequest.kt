package com.jamsell.gethics.livestock.data

data class RegisterAnimalRequest(
    val tag: String,
    val breed: String,
    val birthDate: String,          // LocalDate ISO: yyyy-MM-dd
    val initialWeightKg: Double?    // opcional; mayor a 0, hasta 5 enteros y 2 decimales
)

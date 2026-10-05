package com.jamsell.gethics.livestock.domain.model

data class Animal(
    val id: String,                 // UUID del backend
    val name: String,
    val tag: String,                // arete / codigo
    val breed: String,
    val photoUrl: String? = null,   // URL del backend o, por ahora, la foto local elegida en el telefono
    val birthDate: String? = null,  // ISO yyyy-MM-dd
    val weightKg: Double? = null,
    val qrCode: String? = null,
    val status: String? = null,     // ACTIVE | SOLD | DECEASED | INACTIVE
    val sex: String? = null,        // MALE | FEMALE
    val farmId: String? = null      // UUID de la granja; se cambia desde la asociacion animal-granja (US10)
)

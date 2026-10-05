package com.jamsell.gethics.livestock.domain.model

// TODO: sumar sex y farmId cuando el formulario y las granjas (US09/10) los usen
data class Animal(
    val id: String,                 // UUID del backend
    val name: String,
    val tag: String,                // arete / codigo
    val breed: String,
    val photoUrl: String? = null,   // URL del backend o, por ahora, la foto local elegida en el telefono
    val birthDate: String? = null,  // ISO yyyy-MM-dd
    val weightKg: Double? = null,
    val qrCode: String? = null,
    val status: String? = null      // ACTIVE | SOLD | DECEASED | INACTIVE
)

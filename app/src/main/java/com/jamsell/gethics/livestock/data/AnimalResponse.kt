package com.jamsell.gethics.livestock.data

import com.jamsell.gethics.livestock.domain.model.Animal

/** AnimalResource del backend (Jackson incluye los null). */
data class AnimalResponse(
    val id: String,                 // UUID
    val farmId: String?,
    val tag: String,                // el backend lo guarda en mayusculas
    val qrCode: String,             // GTH-XXXXXXXXXXXX, generado por el backend
    val name: String?,
    val breed: String,
    val sex: String?,               // MALE | FEMALE
    val birthDate: String,          // LocalDate ISO
    val initialWeightKg: Double?,
    val photoUrl: String?,
    val status: String              // ACTIVE | SOLD | DECEASED | INACTIVE
)

/** El Figma no pide nombre: si el backend no trae uno, la lista usa el arete. */
fun AnimalResponse.toAnimal(localPhotoUri: String? = null) = Animal(
    id = id,
    name = name ?: tag,
    tag = tag,
    breed = breed,
    photoUrl = photoUrl ?: localPhotoUri,
    birthDate = birthDate,
    weightKg = initialWeightKg,
    qrCode = qrCode,
    status = status,
    sex = sex,
    farmId = farmId
)

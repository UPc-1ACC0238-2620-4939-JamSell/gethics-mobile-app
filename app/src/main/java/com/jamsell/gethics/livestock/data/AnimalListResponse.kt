package com.jamsell.gethics.livestock.data

/** AnimalListResource (US06). message solo trae valor con la lista vacia ("Sin resultados." o "No hay animales registrados."). */
data class AnimalListResponse(
    val animals: List<AnimalResponse>,
    val message: String?
)

package com.jamsell.gethics.livestock.domain.model

/** Granja asignada a un animal: lo que la app necesita para mostrarla en la ficha. */
data class Farm(
    val id: String,
    val name: String,
    val location: String? = null
)

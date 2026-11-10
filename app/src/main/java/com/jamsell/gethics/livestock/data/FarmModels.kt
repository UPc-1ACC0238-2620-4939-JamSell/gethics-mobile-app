package com.jamsell.gethics.livestock.data

import com.jamsell.gethics.livestock.domain.model.Farm

/** FarmResource del backend. sizeHectares es null si la granja no tiene tamano registrado. */
data class FarmResponse(
    val id: String,
    val ownerId: String,
    val name: String,
    val location: String,
    val sizeHectares: Double?,
    val status: String?
)

/** message solo trae valor cuando farms esta vacio ("No hay granjas registradas."). */
data class FarmListResponse(val farms: List<FarmResponse>, val message: String?)

/** US-32: reemplazo completo de nombre, ubicacion y tamano (tamano null = sin registrar). */
data class UpdateFarmRequest(val name: String, val location: String, val sizeHectares: Double?)

fun FarmResponse.toFarm() = Farm(id, ownerId, name, location, sizeHectares)

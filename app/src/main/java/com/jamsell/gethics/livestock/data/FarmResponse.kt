package com.jamsell.gethics.livestock.data

import com.jamsell.gethics.livestock.domain.model.Farm

/** Granja tal como la devuelve GET api/v1/farms/{id}. */
data class FarmResponse(
    val id: String,
    val ownerId: String,
    val name: String,
    val location: String?,
    val sizeHectares: Double?,
    val status: String
)

fun FarmResponse.toFarm() = Farm(id = id, name = name, location = location)

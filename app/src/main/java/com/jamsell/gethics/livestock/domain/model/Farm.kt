package com.jamsell.gethics.livestock.domain.model

data class Farm(
    val id: String,
    val ownerId: String,
    val name: String,
    val location: String,
    val sizeHectares: Double? = null
)

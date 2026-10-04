package com.jamsell.gethics.livestock.domain.model

// TODO: alinear campos con el aggregate Animal del backend (livestock)
data class Animal(
    val id: Long,
    val name: String,
    val tag: String,        // arete / codigo
    val breed: String,
    val photoUrl: String? = null
)

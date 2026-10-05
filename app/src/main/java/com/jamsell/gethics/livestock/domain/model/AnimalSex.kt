package com.jamsell.gethics.livestock.domain.model

/** Sexo del animal tal como lo envia el backend (MALE | FEMALE). */
enum class AnimalSex(val label: String) {
    MALE("Macho"),
    FEMALE("Hembra")
}

package com.jamsell.gethics.iam.domain.model

/** Mismos valores que Role.java del backend (iam). */
enum class Role(val label: String) {
    GANADERO("Ganadero"),
    VETERINARIO("Veterinario");

    companion object {
        fun from(value: String?): Role =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: GANADERO
    }
}

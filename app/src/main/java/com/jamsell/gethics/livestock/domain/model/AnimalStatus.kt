package com.jamsell.gethics.livestock.domain.model

/** Estados del animal en el backend. El inventario muestra por defecto solo ACTIVE; el resto sale por el filtro. */
enum class AnimalStatus(val label: String) {
    ACTIVE("Activos"),
    SOLD("Vendidos"),
    DECEASED("Fallecidos"),
    INACTIVE("Inactivos");

    companion object {
        /** Estado a partir del texto del backend (ACTIVE, SOLD...). Sin dato o desconocido devuelve null: no se inventa. */
        fun fromName(name: String?): AnimalStatus? = entries.firstOrNull { it.name == name }
    }
}

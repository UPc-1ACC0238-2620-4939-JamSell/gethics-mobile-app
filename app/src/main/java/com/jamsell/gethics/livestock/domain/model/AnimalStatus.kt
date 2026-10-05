package com.jamsell.gethics.livestock.domain.model

/** Estados del animal en el backend. El inventario muestra por defecto solo ACTIVE; el resto sale por el filtro. */
enum class AnimalStatus(val label: String) {
    ACTIVE("Activos"),
    SOLD("Vendidos"),
    DECEASED("Fallecidos"),
    INACTIVE("Inactivos")
}

package com.jamsell.gethics.sanitary.data

/** US46: UpcomingSanitaryEventsResource. message solo trae valor cuando events esta vacio ("No hay alertas pendientes."). */
data class UpcomingSanitaryEventsResponse(
    val today: String,                      // LocalDate ISO del servidor
    val days: Int,
    val events: List<UpcomingSanitaryEventDto>,
    val message: String?
)

/** Ordenados por scheduledDate ascendente: primero los vencidos. daysUntil es negativo si el evento ya vencio. */
data class UpcomingSanitaryEventDto(
    val id: String,               // UUID
    val animalId: String,         // UUID: la pantalla muestra el nombre o arete del animal
    val type: SanitaryEventType?, // null si el backend enviara un tipo que la app no conoce
    val scheduledDate: String,    // LocalDate ISO: yyyy-MM-dd
    val description: String?,
    val daysUntil: Long,
    val overdue: Boolean
)

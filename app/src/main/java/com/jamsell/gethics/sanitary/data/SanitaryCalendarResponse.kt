package com.jamsell.gethics.sanitary.data

/** SanitaryCalendarResource. message solo trae valor cuando events esta vacio ("No hay actividades pendientes."). */
data class SanitaryCalendarResponse(
    val year: Int,
    val month: Int,
    val events: List<ScheduledEventDto>,
    val message: String?
)

/** ScheduledEventResource. Llegan ordenados por scheduledDate ascendente y siempre con status SCHEDULED. */
data class ScheduledEventDto(
    val id: String,               // UUID
    val animalId: String,         // UUID: no se muestra al usuario
    val type: SanitaryEventType?, // null si el backend enviara un tipo que la app no conoce
    val scheduledDate: String,    // LocalDate ISO: yyyy-MM-dd
    val description: String?,
    val status: String
)

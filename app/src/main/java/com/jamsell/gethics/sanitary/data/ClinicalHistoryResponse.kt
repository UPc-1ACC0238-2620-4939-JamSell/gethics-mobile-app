package com.jamsell.gethics.sanitary.data

/** Estados que devuelve el backend. Gson los lee por nombre; un valor desconocido queda en null. */
enum class SanitaryEventStatus(val label: String) {
    SCHEDULED("Programado"),
    COMPLETED("Realizado"),
    CANCELLED("Cancelado")
}

/** ClinicalHistoryResource. message solo trae valor cuando events esta vacio ("Sin registros."). */
data class ClinicalHistoryResponse(
    val animalId: String,
    val events: List<ClinicalHistoryEventDto>,
    val message: String?
)

/** ClinicalHistoryEventResource. Llegan en orden cronologico ascendente (lo garantiza el backend). */
data class ClinicalHistoryEventDto(
    val id: String,                    // UUID: no se muestra al usuario
    val type: SanitaryEventType?,
    val status: SanitaryEventStatus?,
    val occurredAt: String?,           // LocalDateTime ISO; solo en COMPLETED
    val scheduledDate: String?,        // LocalDate ISO; en SCHEDULED/CANCELLED (y en COMPLETED que fue programado)
    val description: String?
)

package com.jamsell.gethics.sanitary.data

/** Tipos que acepta el backend. Gson los envia por nombre (VACCINATION, ...). */
enum class SanitaryEventType(val label: String) {
    VACCINATION("Vacuna"),
    TREATMENT("Tratamiento"),
    DISEASE("Enfermedad"),
    CHECKUP("Chequeo"),
    OTHER("Otro")
}

/** Body de RegisterSanitaryEventResource. animalId va en la URL; el backend asigna status = COMPLETED. */
data class RegisterSanitaryEventRequest(
    val type: SanitaryEventType,
    val occurredAt: String,      // LocalDateTime ISO: yyyy-MM-dd'T'HH:mm:ss
    val description: String?     // opcional; null no se envia
)

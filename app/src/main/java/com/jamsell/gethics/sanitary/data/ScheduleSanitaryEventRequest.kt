package com.jamsell.gethics.sanitary.data

/** Body de ScheduleSanitaryEventResource (POST .../sanitary-events/scheduled). US13 solo programa VACCINATION. */
data class ScheduleSanitaryEventRequest(
    val type: SanitaryEventType,
    val scheduledDate: String,   // LocalDate ISO: yyyy-MM-dd; el backend exige que no sea anterior a hoy
    val description: String?     // opcional; null no se envia
)

/** Body de CompleteSanitaryEventResource (POST .../{eventId}/complete). Sin description el backend conserva la de la programacion. */
data class CompleteSanitaryEventRequest(
    val occurredAt: String,      // LocalDateTime ISO: yyyy-MM-dd'T'HH:mm:ss; el backend exige que no sea futura
    val description: String?
)

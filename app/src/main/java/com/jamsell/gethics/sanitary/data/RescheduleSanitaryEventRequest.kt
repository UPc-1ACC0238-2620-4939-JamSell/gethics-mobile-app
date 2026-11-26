package com.jamsell.gethics.sanitary.data

/** US47: nueva fecha de un evento programado. description null = el backend conserva las notas actuales. */
data class RescheduleSanitaryEventRequest(
    val scheduledDate: String,   // LocalDate ISO: yyyy-MM-dd; el backend exige que no sea anterior a hoy
    val description: String?
)

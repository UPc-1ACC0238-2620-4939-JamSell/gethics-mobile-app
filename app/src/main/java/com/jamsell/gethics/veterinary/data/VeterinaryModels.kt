package com.jamsell.gethics.veterinary.data

data class AssignedClientDto(
    val assignmentId: String,
    val clientId: String,
    val location: String?,
    val assignedAt: String?
)

/** message solo trae valor cuando clients esta vacio ("No linked clients"). */
data class AssignedClientsResponse(val clients: List<AssignedClientDto>, val message: String?)

data class PatientDto(
    val patientId: String,
    val name: String?,
    val tag: String?,
    val breed: String?,
    val status: String?,
    val totalClinicalEvents: Int,
    val lastEventDate: String?,
    val lastEventType: String?
)

data class ClientPatientsResponse(val patients: List<PatientDto>, val message: String?)

data class RegisterCareRequest(
    val clientRequestId: String,
    val veterinarianId: String,
    val diagnosis: String,
    val treatment: String,
    val nextControlDate: String?,
    val occurredAt: String?
)

data class CareRecordDto(
    val id: String,
    val clientRequestId: String,
    val patientId: String,
    val diagnosis: String,
    val treatment: String,
    val nextControlDate: String?,
    val historySynced: Boolean
)
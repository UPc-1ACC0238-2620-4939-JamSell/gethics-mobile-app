package com.jamsell.gethics.veterinary.data.repository

import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.remote.errorMessage
import com.jamsell.gethics.veterinary.data.AssignedClientsResponse
import com.jamsell.gethics.veterinary.data.CareRecordDto
import com.jamsell.gethics.veterinary.data.ClientPatientsResponse
import com.jamsell.gethics.veterinary.data.RegisterCareRequest
import com.jamsell.gethics.veterinary.data.VeterinaryService
import java.io.IOException
import retrofit2.Response

private const val CONNECTION_ERROR = "No se pudo conectar con el servidor"

class VeterinaryRepository(private val service: VeterinaryService) {

    suspend fun getAssignedClients(veterinarianId: String): Resource<AssignedClientsResponse> =
        call { service.getAssignedClients(veterinarianId) }

    suspend fun getClientPatients(clientId: String, veterinarianId: String): Resource<ClientPatientsResponse> =
        call { service.getClientPatients(clientId, veterinarianId) }

    suspend fun registerCare(patientId: String, request: RegisterCareRequest): Resource<CareRecordDto> =
        call { service.registerCare(patientId, request) }

    private suspend fun <T> call(block: suspend () -> Response<T>): Resource<T> =
        try {
            val response = block()
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body)
            else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }
}
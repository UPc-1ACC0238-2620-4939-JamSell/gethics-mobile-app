package com.jamsell.gethics.veterinary.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VeterinaryService {

    @GET("api/v1/vet/clients")
    suspend fun getAssignedClients(@Query("veterinarianId") veterinarianId: String): Response<AssignedClientsResponse>

    @GET("api/v1/vet/clients/{clientId}/animals")
    suspend fun getClientPatients(
        @Path("clientId") clientId: String,
        @Query("veterinarianId") veterinarianId: String
    ): Response<ClientPatientsResponse>

    @POST("api/v1/vet/patients/{patientId}/care")
    suspend fun registerCare(
        @Path("patientId") patientId: String,
        @Body request: RegisterCareRequest
    ): Response<CareRecordDto>
}
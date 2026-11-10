package com.jamsell.gethics.iot.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface IotService {

    /** US23. Vincula un dispositivo por su codigo. 201 con el dispositivo; 400 si falta el codigo. */
    @POST("api/v1/devices/link")
    suspend fun linkDevice(@Body request: LinkDeviceRequest): Response<DeviceResponse>

    /** US23. Dispositivos del ganadero con su estado de conexion. */
    @GET("api/v1/devices")
    suspend fun getDevices(@Query("ownerId") ownerId: String): Response<List<DeviceResponse>>

    /**
     * US37. Lecturas del dispositivo (id = UUID), paginadas y de la mas reciente a la mas antigua.
     * size va de 1 a 100 (400 si se pasa); 404 si el dispositivo no existe; sin lecturas devuelve 200 con content vacio.
     */
    @GET("api/v1/devices/{id}/readings")
    suspend fun getReadings(
        @Path("id") deviceId: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<ReadingsPageResponse>
}

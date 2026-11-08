package com.jamsell.gethics.iam.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface UserService {

    /** US04. Perfil del usuario autenticado (el backend lo toma del token, no hay id en la ruta). */
    @GET("users/me")
    suspend fun getProfile(): Response<UserResponse>

    /** US04. 200 con mensaje y usuario actualizado; 400 si el nombre esta vacio o el telefono no es valido. */
    @PUT("users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ProfileUpdatedResponse>

    // TODO: PUT users/me/photo (multipart "file", maximo 2 MB)
}

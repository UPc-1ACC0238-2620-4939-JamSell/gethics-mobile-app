package com.jamsell.gethics.iam.data.repository

import com.jamsell.gethics.iam.data.UpdateProfileRequest
import com.jamsell.gethics.iam.data.UserResponse
import com.jamsell.gethics.iam.data.UserService
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.remote.errorMessage
import java.io.IOException

private const val CONNECTION_ERROR = "No se pudo conectar con el servidor"

class UserRepository(private val service: UserService) {

    suspend fun getProfile(): Resource<UserResponse> =
        try {
            val response = service.getProfile()
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    /** phone vacio se envia como null para que el backend lo borre. */
    suspend fun updateProfile(name: String, phone: String): Resource<UserResponse> =
        try {
            val response = service.updateProfile(UpdateProfileRequest(name, phone.trim().ifEmpty { null }))
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body.user) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    // TODO: updatePhoto
}

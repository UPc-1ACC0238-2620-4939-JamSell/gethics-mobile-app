package com.jamsell.gethics.iam.data.repository

import com.jamsell.gethics.iam.data.AuthService
import com.jamsell.gethics.iam.data.UpdateProfileRequest
import com.jamsell.gethics.iam.data.UserProfileDto
import com.jamsell.gethics.shared.common.Resource
import kotlinx.coroutines.delay

class UserRepository(
    private val service: AuthService
) {
    suspend fun getUserProfile(userId: String): Resource<UserProfileDto> {
        return try {
            delay(800) // Mock
            Resource.Success(
                UserProfileDto(
                    id = userId,
                    fullName = "Luis Angel Pillaca",
                    email = "luis@gethics.com",
                    phone = "+51 987654321",
                    address = "Lima, Perú"
                )
            )
        } catch (e: Exception) {
            Resource.Error("Error al cargar perfil")
        }
    }

    suspend fun updateProfile(userId: String, request: UpdateProfileRequest): Resource<UserProfileDto> {
        return try {
            delay(1000) // Mock
            Resource.Success(
                UserProfileDto(
                    id = userId,
                    fullName = request.fullName,
                    email = "luis@gethics.com",
                    phone = request.phone,
                    address = request.address
                )
            )
        } catch (e: Exception) {
            Resource.Error("Error al actualizar datos")
        }
    }
}
package com.jamsell.gethics.sanitary.data.repository

import com.jamsell.gethics.sanitary.data.RegisterSanitaryEventRequest
import com.jamsell.gethics.sanitary.data.SanitaryService
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.remote.errorMessage
import java.io.IOException

class SanitaryRepository(private val service: SanitaryService) {

    suspend fun registerEvent(animalId: String, request: RegisterSanitaryEventRequest): Resource<Unit> =
        try {
            val response = service.registerEvent(animalId, request)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error("No se pudo conectar con el servidor")
        }
}

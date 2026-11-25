package com.jamsell.gethics.livestock.data.repository

import com.jamsell.gethics.livestock.data.FarmService
import com.jamsell.gethics.livestock.data.toFarm
import com.jamsell.gethics.livestock.domain.model.Farm
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.remote.errorMessage
import java.io.IOException

/** Granjas. Solo lectura y sin cache: si no hay conexion el llamador decide que mostrar en su lugar. */
class FarmRepository(private val service: FarmService) {

    suspend fun getFarm(id: String): Resource<Farm> =
        try {
            val response = service.getFarm(id)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Resource.Success(body.toFarm())
            } else {
                Resource.Error(response.errorMessage())
            }
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }
}

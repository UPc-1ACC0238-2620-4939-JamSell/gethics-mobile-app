package com.jamsell.gethics.livestock.data.repository

import com.jamsell.gethics.livestock.data.FarmService
import com.jamsell.gethics.livestock.data.UpdateFarmRequest
import com.jamsell.gethics.livestock.data.toFarm
import com.jamsell.gethics.livestock.domain.model.Farm
import com.jamsell.gethics.shared.common.Resource
import com.jamsell.gethics.shared.data.remote.errorMessage
import java.io.IOException

/** US-32. Las granjas no se guardan en Room: ver y editar una granja necesita al backend. */
class FarmRepository(private val service: FarmService) {

    /** El backend aun no tiene GET de granja por id: se busca en las granjas del dueno. */
    suspend fun getFarm(ownerId: String, farmId: String): Resource<Farm> =
        try {
            val response = service.getFarms(ownerId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                body.farms.firstOrNull { it.id == farmId }?.let { Resource.Success(it.toFarm()) }
                    ?: Resource.Error(FARM_NOT_FOUND)
            } else {
                Resource.Error(response.errorMessage())
            }
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }

    /** Escenario 1: guarda nombre, ubicacion y tamano. 409: el nombre ya lo usa otra granja del dueno. */
    suspend fun updateFarm(id: String, name: String, location: String, sizeHectares: Double?): Resource<Farm> =
        try {
            val response = service.updateFarm(id, UpdateFarmRequest(name, location, sizeHectares))
            val body = response.body()
            if (response.isSuccessful && body != null) Resource.Success(body.toFarm()) else Resource.Error(response.errorMessage())
        } catch (e: IOException) {
            Resource.Error(CONNECTION_ERROR)
        }
}

const val FARM_NOT_FOUND = "No se encontró la granja"

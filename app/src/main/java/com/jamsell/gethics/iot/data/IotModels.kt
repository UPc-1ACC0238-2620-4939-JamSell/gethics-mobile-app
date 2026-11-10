package com.jamsell.gethics.iot.data

/** El codigo del dispositivo es obligatorio y no puede superar 50 caracteres (igual que el backend). */
const val MAX_DEVICE_CODE_LENGTH = 50

fun isValidDeviceCode(code: String): Boolean = code.isNotBlank() && code.trim().length <= MAX_DEVICE_CODE_LENGTH

data class LinkDeviceRequest(val ownerId: String, val code: String)

/** status llega como "CONNECTED" o "DISCONNECTED". Las fechas llegan ISO sin zona (yyyy-MM-dd'T'HH:mm:ss). */
data class DeviceResponse(
    val id: String,
    val ownerId: String,
    val code: String,
    val status: String,
    val linkedAt: String?,
    val lastReadingAt: String?
) {
    val isConnected: Boolean get() = status == "CONNECTED"
}

data class SensorReadingResponse(
    val id: String,
    val metric: String,
    val value: Double,
    val recordedAt: String
)

/** US37. Pagina de lecturas, de la mas reciente a la mas antigua. page empieza en 0. */
data class ReadingsPageResponse(
    val content: List<SensorReadingResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    val hasMore: Boolean get() = page + 1 < totalPages
}

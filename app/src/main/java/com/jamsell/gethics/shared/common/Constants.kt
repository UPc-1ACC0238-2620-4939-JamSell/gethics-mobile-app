package com.jamsell.gethics.shared.common

object Constants {
    // 10.0.2.2 = localhost de tu PC visto desde el emulador de Android (el backend corre en el 8080).
    // En celular fisico: usar la IP de tu PC en la red WiFi (ej. http://192.168.1.10:8080/)
    // Debe terminar en "/". Los Service declaran rutas relativas con el prefijo del backend (ej. "api/v1/animals/...").
    const val BASE_URL = "https://gethics-backend-staging.onrender.com/"
    // Token de pago de la pasarela SANDBOX del backend (tok_declined / tok_insufficient_funds para probar errores)
    const val SANDBOX_PAYMENT_TOKEN = "tok_approved"
    const val DEV_VET_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    const val DEV_OWNER_ID = "11111111-1111-1111-1111-111111111111"
}

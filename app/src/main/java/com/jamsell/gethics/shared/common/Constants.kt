package com.jamsell.gethics.shared.common

object Constants {
    // 10.0.2.2 = localhost de tu PC visto desde el emulador de Android (el backend corre en el 8080).
    // En celular fisico: usar la IP de tu PC en la red WiFi (ej. http://192.168.1.10:8080/)
    // El backend NO usa prefijo (/auth/login, /plans, /users/me ...). Debe terminar en "/".
    const val BASE_URL = "http://10.0.2.2:8080/"

    // Token de pago de la pasarela SANDBOX del backend (tok_declined / tok_insufficient_funds para probar errores)
    const val SANDBOX_PAYMENT_TOKEN = "tok_approved"
}

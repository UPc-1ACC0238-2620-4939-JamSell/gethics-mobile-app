package com.jamsell.gethics.shared.common

object Constants {
    // Backend de staging en Render (HTTPS). Render Free puede tardar en responder la primera peticion tras estar inactivo.
    // Backend local: "http://10.0.2.2:8080/" (localhost de tu PC visto desde el emulador; en celular fisico, la IP
    // de tu PC en la red WiFi, ej. http://192.168.1.10:8080/). Ojo: HTTP local necesita habilitar cleartext.
    // Debe terminar en "/". Los Service declaran rutas relativas con el prefijo del backend (ej. "api/v1/animals/...").
    const val BASE_URL = "https://gethics-backend-staging-7qth.onrender.com/"

    // UUID del dueño (owner) que usan finanzas, reportes y notificaciones. Mientras el login del backend (IAM) no
    // devuelva el UUID real del usuario, se usa este de prueba. Cuando exista: SessionStorage.saveOwnerId(uuid) al iniciar sesion.
    const val DEFAULT_OWNER_ID = "00000000-0000-0000-0000-000000000001"

    // Token de pago de la pasarela SANDBOX del backend (tok_declined / tok_insufficient_funds para probar errores)
    const val SANDBOX_PAYMENT_TOKEN = "tok_approved"
}

package com.jamsell.gethics.shared.data.local

import android.content.SharedPreferences
import com.jamsell.gethics.shared.common.Constants

/**
 * Guarda el token JWT y el rol del usuario logueado.
 * Lo lee AuthInterceptor (para el header Authorization) y la navegacion (para elegir tabs segun rol).
 * Recibe las SharedPreferences por constructor para poder testearlo sin Context.
 */
class SessionStorage(private val prefs: SharedPreferences) {

    fun saveSession(token: String, userId: Long, role: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_ROLE, role)
            .apply()
    }

    /** UUID del usuario en el backend (ownerId). Si el login aun no lo guardo, se usa Constants.DEFAULT_OWNER_ID. */
    fun saveOwnerId(ownerId: String) = prefs.edit().putString(KEY_OWNER_ID, ownerId).apply()
    fun getOwnerId(): String = prefs.getString(KEY_OWNER_ID, null) ?: Constants.DEFAULT_OWNER_ID

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)
    fun getRole(): String? = prefs.getString(KEY_ROLE, null)
    fun isLoggedIn(): Boolean = getToken() != null

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "user_id"
        const val KEY_ROLE = "role"
        const val KEY_OWNER_ID = "owner_id"
    }
}

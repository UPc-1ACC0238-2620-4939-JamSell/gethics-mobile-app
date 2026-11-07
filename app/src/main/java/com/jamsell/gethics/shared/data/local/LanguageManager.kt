package com.jamsell.gethics.shared.data.local

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

/**
 * Idioma de la app (ES / EN) elegido por el usuario en Ajustes. Sin dependencias extra:
 * se guarda en SharedPreferences y MainActivity envuelve su Context con ese idioma (attachBaseContext).
 * Sin eleccion guardada (null) la app sigue el idioma del telefono.
 * Va aparte de SessionStorage a proposito: cerrar sesion no debe borrar el idioma.
 */
object LanguageManager {
    const val SPANISH = "es"
    const val ENGLISH = "en"

    private const val PREFS = "gethics_settings"
    private const val KEY_LANGUAGE = "language"

    /** "es", "en" o null (idioma del telefono). */
    fun saved(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)

    private fun save(context: Context, code: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LANGUAGE, code).apply()
    }

    /** Context con el idioma guardado. Si no hay uno, devuelve el mismo context. */
    fun wrap(base: Context): Context {
        val code = saved(base) ?: return base
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    /** Guarda el idioma y recrea la Activity para que toda la UI se vuelva a dibujar en el idioma nuevo. */
    fun change(context: Context, code: String?) {
        save(context, code)
        var current: Context = context
        while (current is ContextWrapper) {
            if (current is Activity) {
                current.recreate()
                return
            }
            current = current.baseContext
        }
    }
}

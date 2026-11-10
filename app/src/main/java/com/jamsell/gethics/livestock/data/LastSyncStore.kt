package com.jamsell.gethics.livestock.data

import android.content.SharedPreferences

/** US-36. Momento (epoch millis) en que los animales se trajeron del backend por ultima vez; null si nunca. */
interface LastSyncStore {
    fun save(epochMillis: Long)
    fun get(): Long?
}

class PrefsLastSyncStore(private val prefs: SharedPreferences) : LastSyncStore {
    override fun save(epochMillis: Long) = prefs.edit().putLong(KEY, epochMillis).apply()
    override fun get(): Long? = if (prefs.contains(KEY)) prefs.getLong(KEY, 0L) else null

    private companion object {
        const val KEY = "animals_last_sync"
    }
}

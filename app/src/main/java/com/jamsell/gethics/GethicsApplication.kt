package com.jamsell.gethics

import android.app.Application
import com.jamsell.gethics.sanitary.data.sync.enqueueSanitarySync
import com.jamsell.gethics.shared.di.AppContainer

class GethicsApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
        // US35: recupera operaciones de sanidad que quedaron pendientes si la app se cerro sin conexion.
        // Es trabajo unico: no duplica workers y, con la cola vacia, termina enseguida.
        enqueueSanitarySync(this)
    }
}

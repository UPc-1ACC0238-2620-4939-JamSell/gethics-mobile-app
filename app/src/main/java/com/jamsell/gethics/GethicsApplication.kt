package com.jamsell.gethics

import android.app.Application
import com.jamsell.gethics.shared.di.AppContainer

class GethicsApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}

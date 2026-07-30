package com.mitra.learning

import android.app.Application
import com.mitra.learning.core.AppContainer

class MitraApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

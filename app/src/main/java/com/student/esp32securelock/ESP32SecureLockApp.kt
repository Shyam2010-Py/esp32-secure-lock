package com.student.esp32securelock

import android.app.Application
import com.student.esp32securelock.data.AppContainer

class ESP32SecureLockApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

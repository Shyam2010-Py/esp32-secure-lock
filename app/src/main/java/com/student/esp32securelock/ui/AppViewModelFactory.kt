package com.student.esp32securelock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.student.esp32securelock.ESP32SecureLockApp

/**
 * Factory that creates AppViewModel using the application's
 * AppContainer.
 */
class AppViewModelFactory(
    private val application: ESP32SecureLockApp
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (!modelClass.isAssignableFrom(AppViewModel::class.java)) {
            throw IllegalArgumentException(
                "Unknown ViewModel class: ${modelClass.name}"
            )
        }

        return AppViewModel(
            store = application.container.passcodeStore,
            esp32 = application.container.esp32Authenticator
        ) as T
    }
}
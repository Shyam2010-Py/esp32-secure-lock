package com.student.esp32securelock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.student.esp32securelock.ESP32SecureLockApp

/**
 * Factory that wires the [AppContainer] into the [AppViewModel].
 */
object AppViewModelFactory {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = (this[APPLICATION_KEY] as ESP32SecureLockApp)
            AppViewModel(app.container.passcodeStore, app.container.esp32Authenticator)
        }
    }
}

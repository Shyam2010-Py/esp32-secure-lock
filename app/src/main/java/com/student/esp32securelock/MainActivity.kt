package com.student.esp32securelock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.student.esp32securelock.data.AppAuthState
import com.student.esp32securelock.navigation.Destination
import com.student.esp32securelock.ui.AppViewModel
import com.student.esp32securelock.ui.AppViewModelFactory
import com.student.esp32securelock.ui.screens.LockScreen
import com.student.esp32securelock.ui.screens.MainScreen
import com.student.esp32securelock.ui.screens.SetupScreen
import com.student.esp32securelock.ui.theme.ESP32SecureLockTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(application as ESP32SecureLockApp)
    }

    private val processLifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                viewModel.onProcessBackgrounded()
            }

            else -> {
                // No action required.
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(processLifecycleObserver)

        setContent {
            ESP32SecureLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppRoot(viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get()
            .lifecycle
            .removeObserver(processLifecycleObserver)

        super.onDestroy()
    }
}

@Composable
private fun AppRoot(viewModel: AppViewModel) {

    val state by viewModel.state.collectAsState()

    val destination = when (state) {
        AppAuthState.NeedsSetup -> Destination.Setup
        AppAuthState.Locked -> Destination.Lock
        AppAuthState.Unlocked -> Destination.Main
    }

    when (destination) {

        Destination.Setup -> {
            SetupScreen(viewModel) {
                // Navigation is controlled by the state flow.
            }
        }

        Destination.Lock -> {
            LockScreen(viewModel) {
                // Navigation is controlled by the state flow.
            }
        }

        Destination.Main -> {
            MainScreen(viewModel) {
                viewModel.lock()
            }
        }
    }
}
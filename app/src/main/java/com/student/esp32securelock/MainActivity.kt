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

    private val viewModel: AppViewModel by viewModels { AppViewModelFactory.Factory }

    /**
     * Watches the *process* lifecycle (not the activity lifecycle) for
     * ON_STOP / ON_START transitions. The process-level ON_STOP fires
     * only when the entire app process is sent to the background — i.e.
     * every visible activity has been stopped. Critically, it does NOT
     * fire on:
     *
     *   - configuration changes (rotation, dark-mode toggle, font
     *     scale change, locale change, etc.),
     *   - transient activity recreation,
     *   - permission dialogs that only pause the foreground activity.
     *
     * The observer is registered once and torn down with the activity,
     * so it does not leak across configuration changes.
     */
    private val processLifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> viewModel.onProcessBackgrounded()
            else -> { /* no-op: we only care about the process going to background */ }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)
        setContent {
            ESP32SecureLockTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processLifecycleObserver)
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
        Destination.Setup -> SetupScreen(viewModel) { /* state flow drives nav */ }
        Destination.Lock -> LockScreen(viewModel) { /* state flow drives nav */ }
        Destination.Main -> MainScreen(viewModel) { viewModel.lock() }
    }
}

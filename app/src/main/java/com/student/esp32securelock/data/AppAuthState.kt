package com.student.esp32securelock.data

/**
 * App-level auth state observed by the UI.
 */
sealed class AppAuthState {
    object NeedsSetup : AppAuthState()
    object Locked : AppAuthState()
    object Unlocked : AppAuthState()
}

package com.student.esp32securelock.navigation

/**
 * Single source of truth for the in-app destinations.
 * We keep navigation simple: a sealed hierarchy of states instead of
 * Routes-with-arguments, because this app is essentially a wizard.
 */
sealed class Destination {
    object Setup : Destination()
    object Lock : Destination()
    object Main : Destination()
}

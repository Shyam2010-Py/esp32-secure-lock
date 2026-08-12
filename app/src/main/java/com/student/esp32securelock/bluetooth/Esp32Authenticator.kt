package com.student.esp32securelock.bluetooth

/**
 * Abstraction over the (future) ESP32 Bluetooth authentication channel.
 *
 * Version 1 ships only this interface plus a [BluetoothServiceProvider] that
 * returns a [NotConfiguredEsp32Authenticator]. A later release will provide
 * a real implementation backed by Android's Bluetooth/BLE stack talking to
 * an ESP32 that returns an authentication response.
 *
 * Flow (planned):
 *
 *   Android App
 *        ↓
 *   Bluetooth / BLE
 *        ↓
 *   ESP32
 *        ↓
 *   ESP32 authentication response
 *        ↓
 *   Android authentication decision
 */
interface Esp32Authenticator {

    /** Stable, user-visible status text shown on the dashboard. */
    val statusText: String

    /**
     * Attempts a Bluetooth connection. The current implementation never
     * succeeds and simply reports "Not configured".
     */
    suspend fun connect(): ConnectionResult
}

sealed class ConnectionResult {
    object NotConfigured : ConnectionResult()
    data class Error(val message: String) : ConnectionResult()
}

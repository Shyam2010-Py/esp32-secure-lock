package com.student.esp32securelock.bluetooth

/**
 * Provider that returns the current [Esp32Authenticator] implementation.
 *
 * Today this returns [NotConfiguredEsp32Authenticator]. In a future version,
 * the real BLE implementation will be wired in here.
 */
object BluetoothServiceProvider {
    fun provide(): Esp32Authenticator = NotConfiguredEsp32Authenticator
}

/**
 * Placeholder authenticator used by Version 1. It never reports a successful
 * connection; the dashboard will display "ESP32 connection: Not configured".
 */
object NotConfiguredEsp32Authenticator : Esp32Authenticator {
    override val statusText: String = "ESP32 connection: Not configured"
    override suspend fun connect(): ConnectionResult = ConnectionResult.NotConfigured
}

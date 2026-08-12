package com.student.esp32securelock.bluetooth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NotConfiguredEsp32AuthenticatorTest {

    @Test
    fun `status text reports not configured`() {
        val auth: Esp32Authenticator = NotConfiguredEsp32Authenticator
        assertEquals("ESP32 connection: Not configured", auth.statusText)
    }

    @Test
    fun `connect returns NotConfigured`() = runTest {
        val res = NotConfiguredEsp32Authenticator.connect()
        assertEquals(ConnectionResult.NotConfigured, res)
    }
}

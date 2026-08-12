package com.student.esp32securelock.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasscodeHasherTest {

    @Test
    fun `hash produces non-empty salt and hash and matching iterations`() {
        val h = PasscodeHasher.hash("1234")
        assertTrue("salt must not be empty", h.salt.isNotEmpty())
        assertTrue("hash must not be empty", h.hash.isNotEmpty())
        assertEquals(120_000, h.iterations)
    }

    @Test
    fun `same passcode twice produces different hash because of random salt`() {
        val a = PasscodeHasher.hash("1234")
        val b = PasscodeHasher.hash("1234")
        assertNotEquals("salts must differ", a.salt, b.salt)
        assertNotEquals("hashes must differ because salts differ", a.hash, b.hash)
    }

    @Test
    fun `verify accepts the correct passcode`() {
        val h = PasscodeHasher.hash("9876")
        assertTrue(PasscodeHasher.verify("9876", h))
    }

    @Test
    fun `verify rejects an incorrect passcode`() {
        val h = PasscodeHasher.hash("9876")
        assertEquals(false, PasscodeHasher.verify("1111", h))
    }

    @Test
    fun `verify rejects empty passcode`() {
        val h = PasscodeHasher.hash("9876")
        assertEquals(false, PasscodeHasher.verify("", h))
    }

    @Test
    fun `hash supports the 32 digit maximum length`() {
        val pin = "1".repeat(32)
        val h = PasscodeHasher.hash(pin)
        assertTrue(PasscodeHasher.verify(pin, h))
        assertEquals(false, PasscodeHasher.verify("1".repeat(31), h))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `hash rejects empty passcode as a defensive guard`() {
        PasscodeHasher.hash("")
    }
}

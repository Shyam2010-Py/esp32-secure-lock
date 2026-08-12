package com.student.esp32securelock.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasscodeValidatorTest {

    // ----- length boundaries -----

    @Test
    fun `empty passcode is invalid`() {
        val r = PasscodeValidator.validate("")
        assertTrue(r is PasscodeValidator.Result.Invalid)
        assertEquals(PasscodeValidator.Reason.EMPTY,
            (r as PasscodeValidator.Result.Invalid).reason)
    }

    @Test
    fun `3 digit passcode is too short`() {
        val r = PasscodeValidator.validate("123")
        assertTrue(r is PasscodeValidator.Result.Invalid)
        assertEquals(PasscodeValidator.Reason.TOO_SHORT,
            (r as PasscodeValidator.Result.Invalid).reason)
    }

    @Test
    fun `4 digit passcode is accepted`() {
        assertEquals(PasscodeValidator.Result.Ok, PasscodeValidator.validate("1234"))
    }

    @Test
    fun `normal 6 digit passcode is accepted`() {
        assertEquals(PasscodeValidator.Result.Ok, PasscodeValidator.validate("123456"))
    }

    @Test
    fun `32 digit passcode is accepted`() {
        val pin = "1".repeat(32)
        assertEquals(PasscodeValidator.Result.Ok, PasscodeValidator.validate(pin))
    }

    @Test
    fun `33 digit passcode is too long`() {
        val pin = "1".repeat(33)
        val r = PasscodeValidator.validate(pin)
        assertTrue(r is PasscodeValidator.Result.Invalid)
        assertEquals(PasscodeValidator.Reason.TOO_LONG,
            (r as PasscodeValidator.Result.Invalid).reason)
    }

    // ----- character class -----

    @Test
    fun `letters are rejected`() {
        val r = PasscodeValidator.validate("abcd")
        assertTrue(r is PasscodeValidator.Result.Invalid)
        assertEquals(PasscodeValidator.Reason.NOT_NUMERIC,
            (r as PasscodeValidator.Result.Invalid).reason)
    }

    @Test
    fun `letters mixed with digits are rejected`() {
        val r = PasscodeValidator.validate("12ab34")
        assertTrue(r is PasscodeValidator.Result.Invalid)
        assertEquals(PasscodeValidator.Reason.NOT_NUMERIC,
            (r as PasscodeValidator.Result.Invalid).reason)
    }

    @Test
    fun `symbols are rejected`() {
        val r = PasscodeValidator.validate("12#56")
        assertTrue(r is PasscodeValidator.Result.Invalid)
        assertEquals(PasscodeValidator.Reason.NOT_NUMERIC,
            (r as PasscodeValidator.Result.Invalid).reason)
    }

    @Test
    fun `whitespace is rejected`() {
        val r = PasscodeValidator.validate("12 56")
        assertTrue(r is PasscodeValidator.Result.Invalid)
        assertEquals(PasscodeValidator.Reason.NOT_NUMERIC,
            (r as PasscodeValidator.Result.Invalid).reason)
    }

    // ----- confirmation -----

    @Test
    fun `confirmation matches when equal`() {
        assertTrue(PasscodeValidator.confirmationMatches("1234", "1234"))
    }

    @Test
    fun `confirmation does not match when different`() {
        assertFalse(PasscodeValidator.confirmationMatches("1234", "4321"))
    }
}

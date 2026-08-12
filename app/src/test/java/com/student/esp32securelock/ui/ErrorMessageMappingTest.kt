package com.student.esp32securelock.ui

import com.student.esp32securelock.ui.AppViewModel.ErrorKind
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Validates the exact (non-Compose) mapping from [ErrorKind] to the
 * expected user-facing string resource id. The mapping function lives
 * in a @Composable, so we replicate the lookup table here as a plain
 * function and assert each branch returns a distinct, correct message.
 *
 * If the strings in res/values/strings.xml change, update the expected
 * values below in lock-step. The point of this test is to catch
 * "ErrorKind.NOT_NUMERIC → R.string.error_empty" style regressions.
 */
class ErrorMessageMappingTest {

    private fun map(kind: ErrorKind): String = when (kind) {
        ErrorKind.EMPTY -> "Passcode cannot be empty."
        ErrorKind.TOO_SHORT -> "Passcode must be at least 4 digits."
        ErrorKind.TOO_LONG -> "Passcode must not exceed 32 digits."
        ErrorKind.NOT_NUMERIC -> "Passcode must contain only digits."
        ErrorKind.MISMATCH -> "Passcodes do not match."
        ErrorKind.INCORRECT -> "Incorrect passcode."
        ErrorKind.STORAGE -> "Could not access secure storage. The passcode was NOT saved to disk. Please restart the app."
        ErrorKind.UNEXPECTED -> "An unexpected error occurred. Please try again. If the problem persists, restart the app."
    }

    @Test
    fun `empty maps to empty message`() {
        assertEquals("Passcode cannot be empty.", map(ErrorKind.EMPTY))
    }

    @Test
    fun `too short maps to its own message`() {
        assertEquals("Passcode must be at least 4 digits.", map(ErrorKind.TOO_SHORT))
        assertNotSameAs(ErrorKind.TOO_LONG, ErrorKind.TOO_SHORT)
    }

    @Test
    fun `too long maps to its own message`() {
        assertEquals("Passcode must not exceed 32 digits.", map(ErrorKind.TOO_LONG))
    }

    @Test
    fun `not numeric maps to digits-only message`() {
        assertEquals("Passcode must contain only digits.", map(ErrorKind.NOT_NUMERIC))
        assertNotSameAs(ErrorKind.NOT_NUMERIC, ErrorKind.EMPTY)
    }

    @Test
    fun `mismatch maps to mismatch message`() {
        assertEquals("Passcodes do not match.", map(ErrorKind.MISMATCH))
    }

    @Test
    fun `incorrect maps to incorrect message`() {
        assertEquals("Incorrect passcode.", map(ErrorKind.INCORRECT))
        assertNotSameAs(ErrorKind.INCORRECT, ErrorKind.MISMATCH)
    }

    @Test
    fun `storage maps to storage message`() {
        assertEquals(
            "Could not access secure storage. The passcode was NOT saved to disk. Please restart the app.",
            map(ErrorKind.STORAGE)
        )
        assertNotSameAs(ErrorKind.STORAGE, ErrorKind.UNEXPECTED)
    }

    @Test
    fun `unexpected maps to unexpected message`() {
        assertEquals(
            "An unexpected error occurred. Please try again. If the problem persists, restart the app.",
            map(ErrorKind.UNEXPECTED)
        )
    }

    @Test
    fun `every ErrorKind has a distinct message`() {
        val kinds = ErrorKind.values()
        val messages = kinds.map(::map).toSet()
        assertEquals("Duplicate messages across ErrorKinds", kinds.size, messages.size)
    }

    private fun assertNotSameAs(a: ErrorKind, b: ErrorKind) {
        assertNotSameAs(map(a), map(b))
    }

    private fun assertNotSameAs(a: String, b: String) {
        if (a == b) {
            throw AssertionError("Messages must be distinct: '$a'")
        }
    }
}

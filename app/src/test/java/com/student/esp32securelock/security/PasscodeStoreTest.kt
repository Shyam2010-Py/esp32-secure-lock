package com.student.esp32securelock.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasscodeStoreTest {

    @Test
    fun `initially no passcode is set`() {
        val store = FakeRepository()
        assertFalse(store.isPasscodeSet())
    }

    @Test
    fun `saving a passcode marks the store as configured and verifies correctly`() {
        val store = FakeRepository()
        assertTrue(store.savePasscode("1234"))
        assertTrue(store.isPasscodeSet())
        assertTrue(store.verifyPasscode("1234"))
        assertFalse(store.verifyPasscode("0000"))
    }

    @Test
    fun `changing passcode requires current to match`() {
        val store = FakeRepository()
        store.savePasscode("1111")
        assertEquals(ChangePasscodeResult.Success, store.changePasscode("1111", "2222"))
        assertTrue(store.verifyPasscode("2222"))
        assertFalse(store.verifyPasscode("1111"))
    }

    @Test
    fun `changing passcode rejects wrong current`() {
        val store = FakeRepository()
        store.savePasscode("1111")
        assertEquals(
            ChangePasscodeResult.IncorrectCurrent,
            store.changePasscode("9999", "2222")
        )
        assertTrue(store.verifyPasscode("1111"))
    }

    @Test
    fun `removing passcode requires current to match`() {
        val store = FakeRepository()
        store.savePasscode("1111")
        assertEquals(RemovePasscodeResult.Success, store.removePasscode("1111"))
        assertFalse(store.isPasscodeSet())
        assertFalse(store.verifyPasscode("1111"))
    }

    @Test
    fun `removing passcode rejects wrong current`() {
        val store = FakeRepository()
        store.savePasscode("1111")
        assertEquals(
            RemovePasscodeResult.IncorrectCurrent,
            store.removePasscode("0000")
        )
        assertTrue(store.isPasscodeSet())
    }

    private class FakeRepository : PasscodeRepository {
        private var hashed: PasscodeHasher.HashedPasscode? = null
        override fun isPasscodeSet(): Boolean = hashed != null
        override fun savePasscode(passcode: String): Boolean {
            if (PasscodeValidator.validate(passcode) !is PasscodeValidator.Result.Ok) return false
            hashed = PasscodeHasher.hash(passcode)
            return true
        }
        override fun verifyPasscode(passcode: String): Boolean =
            hashed?.let { PasscodeHasher.verify(passcode, it) } ?: false
        override fun changePasscode(
            currentPasscode: String,
            newPasscode: String
        ): ChangePasscodeResult {
            if (!verifyPasscode(currentPasscode)) return ChangePasscodeResult.IncorrectCurrent
            return if (savePasscode(newPasscode)) ChangePasscodeResult.Success
                   else ChangePasscodeResult.StorageFailure
        }
        override fun removePasscode(currentPasscode: String): RemovePasscodeResult {
            if (!verifyPasscode(currentPasscode)) return RemovePasscodeResult.IncorrectCurrent
            hashed = null
            return RemovePasscodeResult.Success
        }
        override fun forceRemove() { hashed = null }
    }
}

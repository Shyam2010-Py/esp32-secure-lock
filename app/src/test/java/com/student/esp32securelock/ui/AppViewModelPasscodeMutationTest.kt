package com.student.esp32securelock.ui

import com.student.esp32securelock.bluetooth.ConnectionResult
import com.student.esp32securelock.bluetooth.Esp32Authenticator
import com.student.esp32securelock.data.AppAuthState
import com.student.esp32securelock.security.ChangePasscodeResult
import com.student.esp32securelock.security.PasscodeHasher
import com.student.esp32securelock.security.PasscodeRepository
import com.student.esp32securelock.security.PasscodeValidator
import com.student.esp32securelock.security.RemovePasscodeResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that changePasscode() and removePasscode() correctly
 * distinguish the failure modes of [PasscodeRepository]:
 *
 *  - Wrong current passcode → ErrorKind.INCORRECT
 *  - Secure-storage write failure → ErrorKind.STORAGE
 *  - Unexpected internal error → ErrorKind.UNEXPECTED
 *
 * The previous implementation collapsed all failures into INCORRECT,
 * which is the exact bug this test file guards against.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelPasscodeMutationTest {

    // ----- changePasscode ------------------------------------------------

    @Test
    fun `changePasscode with wrong current reports INCORRECT`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val repo = ScriptedRepository(
            initialHashed = PasscodeHasher.hash("1111"),
            changeResult = ChangePasscodeResult.IncorrectCurrent,
            removeResult = RemovePasscodeResult.IncorrectCurrent
        )
        val vm = AppViewModel(repo, FakeEsp32(), dispatcher)

        var successCalled = false
        vm.changePasscode("0000", "2222", "2222") { successCalled = true }
        advanceUntilIdle()

        assertEquals(AppViewModel.ErrorKind.INCORRECT, vm.ui.value.error)
        assertFalse("onSuccess must not be called on wrong current", successCalled)
        // Original passcode is still active.
        assertTrue(repo.verifyPasscode("1111"))
    }

    @Test
    fun `changePasscode with storage failure reports STORAGE not INCORRECT`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val repo = ScriptedRepository(
            initialHashed = PasscodeHasher.hash("1111"),
            changeResult = ChangePasscodeResult.StorageFailure,
            removeResult = RemovePasscodeResult.IncorrectCurrent
        )
        val vm = AppViewModel(repo, FakeEsp32(), dispatcher)

        var successCalled = false
        vm.changePasscode("1111", "2222", "2222") { successCalled = true }
        advanceUntilIdle()

        assertEquals(
            "A storage failure must NOT be reported as INCORRECT",
            AppViewModel.ErrorKind.STORAGE, vm.ui.value.error
        )
        assertFalse(successCalled)
        // Original passcode is still active because the write failed.
        assertTrue(repo.verifyPasscode("1111"))
    }

    @Test
    fun `changePasscode with unexpected failure reports UNEXPECTED`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val repo = ScriptedRepository(
            initialHashed = PasscodeHasher.hash("1111"),
            changeResult = ChangePasscodeResult.Unexpected,
            removeResult = RemovePasscodeResult.IncorrectCurrent
        )
        val vm = AppViewModel(repo, FakeEsp32(), dispatcher)

        vm.changePasscode("1111", "2222", "2222") { /* not called */ }
        advanceUntilIdle()

        assertEquals(AppViewModel.ErrorKind.UNEXPECTED, vm.ui.value.error)
    }

    @Test
    fun `changePasscode success clears any prior error`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val repo = ScriptedRepository(
            initialHashed = PasscodeHasher.hash("1111"),
            changeResult = ChangePasscodeResult.Success,
            removeResult = RemovePasscodeResult.IncorrectCurrent
        )
        val vm = AppViewModel(repo, FakeEsp32(), dispatcher)

        // Seed a stale INCORRECT error.
        vm.unlock("0000") { }
        advanceUntilIdle()
        assertEquals(AppViewModel.ErrorKind.INCORRECT, vm.ui.value.error)

        var successCalled = false
        vm.changePasscode("1111", "2222", "2222") { successCalled = true }
        advanceUntilIdle()

        assertTrue(successCalled)
        assertNull(vm.ui.value.error)
        assertTrue(repo.verifyPasscode("2222"))
    }

    @Test
    fun `changePasscode with mismatched confirmation reports MISMATCH and never touches storage`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val repo = ScriptedRepository(
            initialHashed = PasscodeHasher.hash("1111"),
            changeResult = ChangePasscodeResult.Success,
            removeResult = RemovePasscodeResult.IncorrectCurrent
        )
        val vm = AppViewModel(repo, FakeEsp32(), dispatcher)

        vm.changePasscode("1111", "2222", "9999") { /* not called */ }
        advanceUntilIdle()

        assertEquals(AppViewModel.ErrorKind.MISMATCH, vm.ui.value.error)
        // Original passcode is unchanged.
        assertTrue(repo.verifyPasscode("1111"))
        assertFalse(repo.verifyPasscode("2222"))
    }

    // ----- removePasscode ------------------------------------------------

    @Test
    fun `removePasscode with wrong current reports INCORRECT`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val repo = ScriptedRepository(
            initialHashed = PasscodeHasher.hash("1111"),
            changeResult = ChangePasscodeResult.IncorrectCurrent,
            removeResult = RemovePasscodeResult.IncorrectCurrent
        )
        val vm = AppViewModel(repo, FakeEsp32(), dispatcher)

        var successCalled = false
        vm.removePasscode("0000") { successCalled = true }
        advanceUntilIdle()

        assertEquals(AppViewModel.ErrorKind.INCORRECT, vm.ui.value.error)
        assertFalse(successCalled)
        // Passcode is still set.
        assertTrue(repo.isPasscodeSet())
    }

    @Test
    fun `removePasscode with storage failure reports STORAGE not INCORRECT`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val repo = ScriptedRepository(
            initialHashed = PasscodeHasher.hash("1111"),
            changeResult = ChangePasscodeResult.IncorrectCurrent,
            removeResult = RemovePasscodeResult.StorageFailure
        )
        val vm = AppViewModel(repo, FakeEsp32(), dispatcher)

        var successCalled = false
        vm.removePasscode("1111") { successCalled = true }
        advanceUntilIdle()

        assertEquals(
            "A storage failure must NOT be reported as INCORRECT",
            AppViewModel.ErrorKind.STORAGE, vm.ui.value.error
        )
        assertFalse(successCalled)
        // Passcode must still be set because the remove failed.
        assertTrue(repo.isPasscodeSet())
        // And we must still be in the Unlocked state, not NeedsSetup.
        assertEquals(AppAuthState.Unlocked, vm.state.value)
    }

    @Test
    fun `removePasscode with unexpected failure reports UNEXPECTED`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val repo = ScriptedRepository(
            initialHashed = PasscodeHasher.hash("1111"),
            changeResult = ChangePasscodeResult.IncorrectCurrent,
            removeResult = RemovePasscodeResult.Unexpected
        )
        val vm = AppViewModel(repo, FakeEsp32(), dispatcher)

        vm.removePasscode("1111") { /* not called */ }
        advanceUntilIdle()

        assertEquals(AppViewModel.ErrorKind.UNEXPECTED, vm.ui.value.error)
    }

    // ----- helpers -------------------------------------------------------

    /**
     * A repository where changePasscode() and removePasscode() return
     * whatever the test asked for, regardless of the supplied current
     * passcode. This lets each test exercise exactly one branch of
     * ViewModel error mapping without making the underlying storage
     * fail in an ad-hoc way.
     */
    private class ScriptedRepository(
        initialHashed: PasscodeHasher.HashedPasscode?,
        private val changeResult: ChangePasscodeResult,
        private val removeResult: RemovePasscodeResult
    ) : PasscodeRepository {
        private var hashed: PasscodeHasher.HashedPasscode? = initialHashed

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
            if (changeResult is ChangePasscodeResult.Success) {
                if (!verifyPasscode(currentPasscode)) return ChangePasscodeResult.IncorrectCurrent
                if (!savePasscode(newPasscode)) return ChangePasscodeResult.StorageFailure
            }
            return changeResult
        }

        override fun removePasscode(currentPasscode: String): RemovePasscodeResult {
            if (removeResult is RemovePasscodeResult.Success) {
                if (!verifyPasscode(currentPasscode)) return RemovePasscodeResult.IncorrectCurrent
                hashed = null
            }
            return removeResult
        }

        override fun forceRemove() { hashed = null }
    }

    private class FakeEsp32 : Esp32Authenticator {
        override val statusText = "ESP32 connection: Not configured"
        override suspend fun connect(): ConnectionResult = ConnectionResult.NotConfigured
    }
}

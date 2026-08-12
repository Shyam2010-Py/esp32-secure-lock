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
import org.junit.Test

/**
 * Covers the lifecycle-driven re-lock logic that lives on the
 * [AppViewModel].
 *
 * The activity side now uses [androidx.lifecycle.ProcessLifecycleOwner]
 * to forward ON_STOP events to the ViewModel. We can't exercise
 * ProcessLifecycleOwner in a pure JVM unit test, but we *can* test the
 * ViewModel's `onProcessBackgrounded()` seam directly — that is the
 * single piece of behaviour the activity relies on.
 *
 * What this guards against:
 *
 *  1. Re-locking when no passcode is set (must be a no-op).
 *  2. Re-locking when the user is already on the Lock / Setup screens
 *     (must be a no-op, otherwise the Lock screen would unnecessarily
 *     re-render and could contribute to a recomposition loop).
 *  3. Re-locking when the user is currently Unlocked (must move to
 *     Locked).
 *  4. Cleared transient UI state (loading + error) so a stale error
 *     from a previous screen does not leak into the lock screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelLifecycleLockTest {

    @Test
    fun `onProcessBackgrounded is a no-op when no passcode is set`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val vm = AppViewModel(InMemoryRepo(), FakeEsp32(), dispatcher)
        // Initial state is NeedsSetup because no passcode is set.
        assertEquals(AppAuthState.NeedsSetup, vm.state.value)

        vm.onProcessBackgrounded()
        advanceUntilIdle()

        assertEquals(AppAuthState.NeedsSetup, vm.state.value)
    }

    @Test
    fun `onProcessBackgrounded re-locks an Unlocked app when a passcode is set`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val repo = InMemoryRepo().also { it.savePasscode("1234") }
        val vm = AppViewModel(repo, FakeEsp32(), dispatcher)
        // Unlock first.
        vm.unlock("1234") { }
        advanceUntilIdle()
        assertEquals(AppAuthState.Unlocked, vm.state.value)

        // Simulate the process going to background, then back.
        vm.onProcessBackgrounded()
        advanceUntilIdle()
        assertEquals(AppAuthState.Locked, vm.state.value)
    }

    @Test
    fun `onProcessBackgrounded is a no-op when already Locked`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val repo = InMemoryRepo().also { it.savePasscode("1234") }
        val vm = AppViewModel(repo, FakeEsp32(), dispatcher)
        // App is in Locked state on launch because a passcode exists.
        assertEquals(AppAuthState.Locked, vm.state.value)

        vm.onProcessBackgrounded()
        advanceUntilIdle()
        assertEquals(AppAuthState.Locked, vm.state.value)
    }

    @Test
    fun `onProcessBackgrounded clears transient error and loading state`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val repo = InMemoryRepo().also { it.savePasscode("1234") }
        val vm = AppViewModel(repo, FakeEsp32(), dispatcher)
        // Trigger a transient error: wrong passcode on the lock screen.
        vm.unlock("0000") { }
        advanceUntilIdle()
        assertEquals(AppViewModel.ErrorKind.INCORRECT, vm.ui.value.error)

        // Backgrounding must clear it so the lock screen does not show
        // a stale "Incorrect passcode" message.
        vm.onProcessBackgrounded()
        advanceUntilIdle()
        assertEquals(null, vm.ui.value.error)
        assertEquals(false, vm.ui.value.loading)
    }

    @Test
    fun `successive onProcessBackgrounded calls do not change state after first lock`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val repo = InMemoryRepo().also { it.savePasscode("1234") }
        val vm = AppViewModel(repo, FakeEsp32(), dispatcher)
        vm.unlock("1234") { }
        advanceUntilIdle()

        vm.onProcessBackgrounded()
        advanceUntilIdle()
        // Second background event should not re-set the state to a
        // different value or trigger additional recompositions.
        vm.onProcessBackgrounded()
        advanceUntilIdle()

        assertEquals(AppAuthState.Locked, vm.state.value)
    }

    // ----- helpers -------------------------------------------------------

    private class InMemoryRepo : PasscodeRepository {
        private var hashed: PasscodeHasher.HashedPasscode? = null
        override fun isPasscodeSet(): Boolean = hashed != null
        override fun savePasscode(passcode: String): Boolean {
            if (PasscodeValidator.validate(passcode) !is PasscodeValidator.Result.Ok) return false
            hashed = PasscodeHasher.hash(passcode); return true
        }
        override fun verifyPasscode(passcode: String): Boolean =
            hashed?.let { PasscodeHasher.verify(passcode, it) } ?: false
        override fun changePasscode(
            currentPasscode: String,
            newPasscode: String
        ) = ChangePasscodeResult.Unexpected
        override fun removePasscode(
            currentPasscode: String
        ) = RemovePasscodeResult.Unexpected
        override fun forceRemove() { hashed = null }
    }

    private class FakeEsp32 : Esp32Authenticator {
        override val statusText = "ESP32 connection: Not configured"
        override suspend fun connect(): ConnectionResult = ConnectionResult.NotConfigured
    }
}

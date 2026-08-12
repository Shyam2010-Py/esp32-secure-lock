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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @Test
    fun `initial state is NeedsSetup when no passcode is set`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val store = FakeRepository()
        val vm = AppViewModel(store, FakeEsp32(), dispatcher)
        assertEquals(AppAuthState.NeedsSetup, vm.state.value)
    }

    @Test
    fun `initial state is Locked when a passcode already exists`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val store = FakeRepository().also { it.savePasscode("1234") }
        val vm = AppViewModel(store, FakeEsp32(), dispatcher)
        assertEquals(AppAuthState.Locked, vm.state.value)
    }

    @Test
    fun `setPasscode with valid input transitions to Unlocked`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val store = FakeRepository()
        val vm = AppViewModel(store, FakeEsp32(), dispatcher)
        var called = false
        vm.setPasscode("1234", "1234") { called = true }
        advanceUntilIdle()
        assertTrue(called)
        assertEquals(AppAuthState.Unlocked, vm.state.value)
    }

    @Test
    fun `setPasscode rejects mismatched confirmation`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val store = FakeRepository()
        val vm = AppViewModel(store, FakeEsp32(), dispatcher)
        vm.setPasscode("1234", "9999") { }
        advanceUntilIdle()
        assertEquals(AppAuthState.NeedsSetup, vm.state.value)
        assertEquals(AppViewModel.ErrorKind.MISMATCH, vm.ui.value.error)
    }

    @Test
    fun `setPasscode rejects too short passcode`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val store = FakeRepository()
        val vm = AppViewModel(store, FakeEsp32(), dispatcher)
        vm.setPasscode("12", "12") { }
        advanceUntilIdle()
        assertEquals(AppViewModel.ErrorKind.TOO_SHORT, vm.ui.value.error)
    }

    @Test
    fun `unlock with correct passcode transitions to Unlocked`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val store = FakeRepository().also { it.savePasscode("1234") }
        val vm = AppViewModel(store, FakeEsp32(), dispatcher)
        vm.unlock("1234") { }
        advanceUntilIdle()
        assertEquals(AppAuthState.Unlocked, vm.state.value)
    }

    @Test
    fun `unlock with wrong passcode shows incorrect error`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val store = FakeRepository().also { it.savePasscode("1234") }
        val vm = AppViewModel(store, FakeEsp32(), dispatcher)
        vm.unlock("0000") { }
        advanceUntilIdle()
        assertEquals(AppAuthState.Locked, vm.state.value)
        assertEquals(AppViewModel.ErrorKind.INCORRECT, vm.ui.value.error)
    }

    @Test
    fun `changePasscode replaces an existing passcode`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val store = FakeRepository().also { it.savePasscode("1111") }
        val vm = AppViewModel(store, FakeEsp32(), dispatcher)
        vm.changePasscode("1111", "2222", "2222") { }
        advanceUntilIdle()
        assertTrue(store.verifyPasscode("2222"))
        assertFalse(store.verifyPasscode("1111"))
    }

    @Test
    fun `removePasscode deletes existing passcode`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val store = FakeRepository().also { it.savePasscode("1111") }
        val vm = AppViewModel(store, FakeEsp32(), dispatcher)
        vm.removePasscode("1111") { }
        advanceUntilIdle()
        assertFalse(store.isPasscodeSet())
        assertEquals(AppAuthState.NeedsSetup, vm.state.value)
    }

    @Test
    fun `lock returns to Locked when passcode is set`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val store = FakeRepository().also { it.savePasscode("1111") }
        val vm = AppViewModel(store, FakeEsp32(), dispatcher)
        vm.unlock("1111") { }
        advanceUntilIdle()
        vm.lock()
        assertEquals(AppAuthState.Locked, vm.state.value)
    }

    @Test
    fun `clearError resets transient error`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val store = FakeRepository()
        val vm = AppViewModel(store, FakeEsp32(), dispatcher)
        vm.unlock("") { }
        advanceUntilIdle()
        assertNotNull(vm.ui.value.error)
        vm.clearError()
        assertNull(vm.ui.value.error)
    }

    @Test
    fun `tryConnectEsp32 reports Not configured on placeholder`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val store = FakeRepository()
        val vm = AppViewModel(store, FakeEsp32(), dispatcher)
        vm.tryConnectEsp32()
        advanceUntilIdle()
        assertEquals("Not configured", vm.ui.value.esp32Error)
        assertFalse(vm.ui.value.esp32Connecting)
    }

    private class FakeRepository : PasscodeRepository {
        private var hashed: PasscodeHasher.HashedPasscode? = null
        override fun isPasscodeSet(): Boolean = hashed != null
        override fun savePasscode(passcode: String): Boolean {
            if (PasscodeValidator.validate(passcode) !is PasscodeValidator.Result.Ok) return false
            hashed = PasscodeHasher.hash(passcode); return true
        }
        override fun verifyPasscode(passcode: String): Boolean =
            hashed?.let { PasscodeHasher.verify(passcode, it) } ?: false
        override fun changePasscode(c: String, n: String): ChangePasscodeResult {
            if (!verifyPasscode(c)) return ChangePasscodeResult.IncorrectCurrent
            return if (savePasscode(n)) ChangePasscodeResult.Success
                   else ChangePasscodeResult.StorageFailure
        }
        override fun removePasscode(c: String): RemovePasscodeResult {
            if (!verifyPasscode(c)) return RemovePasscodeResult.IncorrectCurrent
            hashed = null; return RemovePasscodeResult.Success
        }
        override fun forceRemove() { hashed = null }
    }

    private class FakeEsp32 : Esp32Authenticator {
        override val statusText: String = "ESP32 connection: Not configured"
        override suspend fun connect(): ConnectionResult = ConnectionResult.NotConfigured
    }
}

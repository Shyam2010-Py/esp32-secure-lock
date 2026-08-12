package com.student.esp32securelock.ui

import com.student.esp32securelock.bluetooth.ConnectionResult
import com.student.esp32securelock.bluetooth.Esp32Authenticator
import com.student.esp32securelock.security.ChangePasscodeResult
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
 * When the underlying [PasscodeRepository] cannot persist the passcode
 * (e.g. EncryptedSharedPreferences initialization failed), the ViewModel
 * must surface a STORAGE error rather than silently claiming success.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelStorageTest {

    @Test
    fun `setPasscode reports STORAGE error when repository cannot persist`() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher] as CoroutineDispatcher
        val vm = AppViewModel(AlwaysFailingRepository(), FakeEsp32(), dispatcher)
        vm.setPasscode("1234", "1234") { /* should not be called */ }
        advanceUntilIdle()
        assertEquals(AppViewModel.ErrorKind.STORAGE, vm.ui.value.error)
    }

    private class AlwaysFailingRepository : PasscodeRepository {
        override fun isPasscodeSet() = false
        override fun savePasscode(passcode: String): Boolean {
            // Pass validation (so we know the error is purely storage)…
            check(PasscodeValidator.validate(passcode) is PasscodeValidator.Result.Ok)
            // …then fail.
            return false
        }
        override fun verifyPasscode(passcode: String) = false
        override fun changePasscode(
            currentPasscode: String,
            newPasscode: String
        ): ChangePasscodeResult = ChangePasscodeResult.StorageFailure
        override fun removePasscode(currentPasscode: String): RemovePasscodeResult =
            RemovePasscodeResult.StorageFailure
        override fun forceRemove() {}
    }

    private class FakeEsp32 : Esp32Authenticator {
        override val statusText = "ESP32 connection: Not configured"
        override suspend fun connect(): ConnectionResult = ConnectionResult.NotConfigured
    }
}

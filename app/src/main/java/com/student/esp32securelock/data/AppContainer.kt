package com.student.esp32securelock.data

import android.app.Application
import android.util.Log
import com.student.esp32securelock.bluetooth.BluetoothServiceProvider
import com.student.esp32securelock.bluetooth.Esp32Authenticator
import com.student.esp32securelock.security.ChangePasscodeResult
import com.student.esp32securelock.security.PasscodeRepository
import com.student.esp32securelock.security.PasscodeStore
import com.student.esp32securelock.security.RemovePasscodeResult

/**
 * Lightweight service locator. We avoid a full DI framework to keep the
 * project simple and beginner-friendly, while still keeping the rest of
 * the code easy to test.
 *
 * If secure storage cannot be initialised, [passcodeStore] is set to a
 * [FailingPasscodeStore] that always reports [AppAuthState.NeedsSetup]
 * and refuses all write attempts (so the user sees a STORAGE error
 * instead of an app crash on launch).
 */
class AppContainer(application: Application) {
    val passcodeStore: PasscodeRepository = try {
        PasscodeStore(application)
    } catch (t: Throwable) {
        Log.e("AppContainer",
            "Secure storage unavailable at startup: ${t.javaClass.simpleName}")
        FailingPasscodeStore
    }
    val esp32Authenticator: Esp32Authenticator = BluetoothServiceProvider.provide()
}

/**
 * Used only when EncryptedSharedPreferences cannot be created at app
 * start. The ViewModel will see `isPasscodeSet() == false` and the
 * setup screen will appear; the first attempt to save will fail with
 * `ErrorKind.STORAGE`.
 */
private object FailingPasscodeStore : PasscodeRepository {
    override fun isPasscodeSet() = false
    override fun savePasscode(passcode: String) = false
    override fun verifyPasscode(passcode: String) = false
    override fun changePasscode(
        currentPasscode: String,
        newPasscode: String
    ): ChangePasscodeResult = ChangePasscodeResult.StorageFailure
    override fun removePasscode(currentPasscode: String): RemovePasscodeResult =
        RemovePasscodeResult.StorageFailure
    override fun forceRemove() {}
}

package com.student.esp32securelock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.student.esp32securelock.bluetooth.ConnectionResult
import com.student.esp32securelock.bluetooth.Esp32Authenticator
import com.student.esp32securelock.data.AppAuthState
import com.student.esp32securelock.security.ChangePasscodeResult
import com.student.esp32securelock.security.PasscodeRepository
import com.student.esp32securelock.security.PasscodeValidator
import com.student.esp32securelock.security.RemovePasscodeResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel responsible for:
 * - passcode setup
 * - unlocking
 * - changing the passcode
 * - removing the passcode
 * - application locking
 * - ESP32 status
 */
class AppViewModel(
    private val store: PasscodeRepository,
    private val esp32: Esp32Authenticator,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow(
        if (store.isPasscodeSet()) {
            AppAuthState.Locked
        } else {
            AppAuthState.NeedsSetup
        }
    )

    val state: StateFlow<AppAuthState> = _state.asStateFlow()

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    fun isPasscodeSet(): Boolean = store.isPasscodeSet()

    fun setPasscode(
        passcode: String,
        confirm: String,
        onSuccess: () -> Unit
    ) {
        val validation = PasscodeValidator.validate(passcode)

        if (validation is PasscodeValidator.Result.Invalid) {
            _ui.update {
                it.copy(
                    error = validation.reason.toError(),
                    loading = false
                )
            }
            return
        }

        if (!PasscodeValidator.confirmationMatches(passcode, confirm)) {
            _ui.update {
                it.copy(
                    error = ErrorKind.MISMATCH,
                    loading = false
                )
            }
            return
        }

        _ui.update {
            it.copy(
                loading = true,
                error = null
            )
        }

        viewModelScope.launch {
            val success = withContext(ioDispatcher) {
                store.savePasscode(passcode)
            }

            _ui.update {
                if (success) {
                    it.copy(
                        loading = false,
                        error = null
                    )
                } else {
                    it.copy(
                        loading = false,
                        error = ErrorKind.STORAGE
                    )
                }
            }

            if (success) {
                _state.value = AppAuthState.Unlocked
                onSuccess()
            }
        }
    }

    fun unlock(
        passcode: String,
        onSuccess: () -> Unit
    ) {
        if (passcode.isEmpty()) {
            _ui.update {
                it.copy(error = ErrorKind.EMPTY)
            }
            return
        }

        _ui.update {
            it.copy(
                loading = true,
                error = null
            )
        }

        viewModelScope.launch {
            val success = withContext(ioDispatcher) {
                store.verifyPasscode(passcode)
            }

            _ui.update {
                if (success) {
                    it.copy(
                        loading = false,
                        error = null
                    )
                } else {
                    it.copy(
                        loading = false,
                        error = ErrorKind.INCORRECT
                    )
                }
            }

            if (success) {
                _state.value = AppAuthState.Unlocked
                onSuccess()
            }
        }
    }

    fun changePasscode(
        current: String,
        newPasscode: String,
        confirm: String,
        onSuccess: () -> Unit
    ) {
        if (current.isEmpty()) {
            _ui.update {
                it.copy(
                    error = ErrorKind.EMPTY,
                    loading = false
                )
            }
            return
        }

        val validation = PasscodeValidator.validate(newPasscode)

        if (validation is PasscodeValidator.Result.Invalid) {
            _ui.update {
                it.copy(
                    error = validation.reason.toError(),
                    loading = false
                )
            }
            return
        }

        if (!PasscodeValidator.confirmationMatches(newPasscode, confirm)) {
            _ui.update {
                it.copy(
                    error = ErrorKind.MISMATCH,
                    loading = false
                )
            }
            return
        }

        _ui.update {
            it.copy(
                loading = true,
                error = null
            )
        }

        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                store.changePasscode(current, newPasscode)
            }

            _ui.update {
                it.copy(
                    loading = false,
                    error = result.toErrorKind()
                )
            }

            if (result is ChangePasscodeResult.Success) {
                onSuccess()
            }
        }
    }

    fun removePasscode(
        current: String,
        onSuccess: () -> Unit
    ) {
        if (current.isEmpty()) {
            _ui.update {
                it.copy(
                    error = ErrorKind.EMPTY,
                    loading = false
                )
            }
            return
        }

        _ui.update {
            it.copy(
                loading = true,
                error = null
            )
        }

        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                store.removePasscode(current)
            }

            _ui.update {
                it.copy(
                    loading = false,
                    error = result.toErrorKind()
                )
            }

            if (result is RemovePasscodeResult.Success) {
                _state.value = AppAuthState.NeedsSetup
                onSuccess()
            }
        }
    }

    /**
     * Re-lock the application when the process genuinely enters
     * the background.
     */
    fun onProcessBackgrounded() {
        if (
            store.isPasscodeSet() &&
            _state.value is AppAuthState.Unlocked
        ) {
            _state.value = AppAuthState.Locked
        }

        clearTransient()
    }

    fun lock() {
        _state.value =
            if (store.isPasscodeSet()) {
                AppAuthState.Locked
            } else {
                AppAuthState.NeedsSetup
            }

        clearTransient()
    }

    fun clearError() {
        _ui.update {
            it.copy(error = null)
        }
    }

    fun esp32Status(): String = esp32.statusText

    fun tryConnectEsp32() {
        _ui.update {
            it.copy(
                esp32Connecting = true,
                esp32Error = null
            )
        }

        viewModelScope.launch {
            val result = esp32.connect()

            _ui.update {
                when (result) {
                    is ConnectionResult.NotConfigured -> {
                        it.copy(
                            esp32Connecting = false,
                            esp32Error = "Not configured"
                        )
                    }

                    is ConnectionResult.Error -> {
                        it.copy(
                            esp32Connecting = false,
                            esp32Error = result.message
                        )
                    }
                }
            }
        }
    }

    private fun clearTransient() {
        _ui.update {
            it.copy(
                error = null,
                loading = false
            )
        }
    }

    /*
     * IMPORTANT:
     * ErrorKind is nullable because Success means there is no error.
     */
    private fun ChangePasscodeResult.toErrorKind(): ErrorKind? {
        return when (this) {
            ChangePasscodeResult.Success -> null
            ChangePasscodeResult.IncorrectCurrent -> ErrorKind.INCORRECT
            ChangePasscodeResult.StorageFailure -> ErrorKind.STORAGE
            ChangePasscodeResult.Unexpected -> ErrorKind.UNEXPECTED
        }
    }

    private fun RemovePasscodeResult.toErrorKind(): ErrorKind? {
        return when (this) {
            RemovePasscodeResult.Success -> null
            RemovePasscodeResult.IncorrectCurrent -> ErrorKind.INCORRECT
            RemovePasscodeResult.StorageFailure -> ErrorKind.STORAGE
            RemovePasscodeResult.Unexpected -> ErrorKind.UNEXPECTED
        }
    }

    data class UiState(
        val loading: Boolean = false,
        val error: ErrorKind? = null,
        val esp32Connecting: Boolean = false,
        val esp32Error: String? = null
    )

    enum class ErrorKind {
        EMPTY,
        TOO_SHORT,
        TOO_LONG,
        NOT_NUMERIC,
        MISMATCH,
        INCORRECT,
        STORAGE,
        UNEXPECTED
    }

    private fun PasscodeValidator.Reason.toError(): ErrorKind {
        return when (this) {
            PasscodeValidator.Reason.EMPTY -> ErrorKind.EMPTY
            PasscodeValidator.Reason.TOO_SHORT -> ErrorKind.TOO_SHORT
            PasscodeValidator.Reason.TOO_LONG -> ErrorKind.TOO_LONG
            PasscodeValidator.Reason.NOT_NUMERIC -> ErrorKind.NOT_NUMERIC
        }
    }
}
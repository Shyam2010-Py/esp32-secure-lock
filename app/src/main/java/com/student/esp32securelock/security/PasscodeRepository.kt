package com.student.esp32securelock.security

/**
 * Storage-agnostic API for the passcode. Implemented by [PasscodeStore]
 * (production, encrypted + persistent) and easily faked in unit tests.
 *
 * Change and remove operations return [ChangePasscodeResult] /
 * [RemovePasscodeResult] so the ViewModel can distinguish between
 * "wrong current passcode" and "secure storage failure". Collapsing both
 * into a single boolean would incorrectly surface a storage problem to
 * the user as "Incorrect passcode.".
 */
interface PasscodeRepository {
    fun isPasscodeSet(): Boolean
    fun savePasscode(passcode: String): Boolean
    fun verifyPasscode(passcode: String): Boolean
    fun changePasscode(
        currentPasscode: String,
        newPasscode: String
    ): ChangePasscodeResult
    fun removePasscode(currentPasscode: String): RemovePasscodeResult
    fun forceRemove()
}

/**
 * Outcome of [PasscodeRepository.changePasscode].
 *
 * - [Success]: the current passcode matched and the new one was written
 *   to secure storage.
 * - [IncorrectCurrent]: the supplied current passcode did not match.
 *   No write was attempted.
 * - [StorageFailure]: the current passcode matched but the new one
 *   could not be persisted (e.g. EncryptedSharedPreferences write
 *   failed). The previous passcode is left in place.
 * - [Unexpected]: an internal error occurred that is neither an
 *   authentication failure nor a known storage failure. The previous
 *   passcode is left in place.
 */
sealed class ChangePasscodeResult {
    object Success : ChangePasscodeResult()
    object IncorrectCurrent : ChangePasscodeResult()
    object StorageFailure : ChangePasscodeResult()
    object Unexpected : ChangePasscodeResult()
}

/**
 * Outcome of [PasscodeRepository.removePasscode]. See
 * [ChangePasscodeResult] for the meaning of the cases — the semantics
 * are identical, except there is no "new" passcode.
 */
sealed class RemovePasscodeResult {
    object Success : RemovePasscodeResult()
    object IncorrectCurrent : RemovePasscodeResult()
    object StorageFailure : RemovePasscodeResult()
    object Unexpected : RemovePasscodeResult()
}

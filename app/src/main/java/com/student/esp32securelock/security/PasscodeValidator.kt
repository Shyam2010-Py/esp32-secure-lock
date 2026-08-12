package com.student.esp32securelock.security

/**
 * Pure validation rules for passcode input. Extracted from the ViewModel so
 * they can be unit-tested without any Android dependency.
 */
object PasscodeValidator {

    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 32

    sealed class Result {
        object Ok : Result()
        data class Invalid(val reason: Reason) : Result()
    }

    enum class Reason {
        EMPTY,
        TOO_SHORT,
        TOO_LONG,
        NOT_NUMERIC
    }

    fun validate(passcode: String): Result {
        if (passcode.isEmpty()) return Result.Invalid(Reason.EMPTY)
        if (passcode.length < MIN_LENGTH) return Result.Invalid(Reason.TOO_SHORT)
        if (passcode.length > MAX_LENGTH) return Result.Invalid(Reason.TOO_LONG)
        if (!passcode.all { it.isDigit() }) return Result.Invalid(Reason.NOT_NUMERIC)
        return Result.Ok
    }

    fun confirmationMatches(passcode: String, confirmation: String): Boolean =
        passcode == confirmation
}

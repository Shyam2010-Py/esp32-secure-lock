package com.student.esp32securelock.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.student.esp32securelock.security.PasscodeHasher.HashedPasscode

/**
 * Encrypted persistent implementation of [PasscodeRepository].
 *
 * Uses AndroidX [EncryptedSharedPreferences] so the underlying file is
 * AES-256-GCM encrypted. We never write the plain-text passcode anywhere.
 *
 * Security policy: if EncryptedSharedPreferences cannot be initialized
 * (corrupt keystore, misconfigured emulator, locked device, etc.) we DO
 * NOT silently fall back to plaintext persistent storage. Instead we throw
 * [SecureStorageUnavailableException]; the UI shows an error and the user
 * knows the passcode is not being persisted.
 *
 * The legacy in-memory implementation is kept ONLY for unit tests that
 * construct [PasscodeRepository] directly. It is not wired into the
 * production code path.
 *
 * Stored keys (kept short, non-descriptive):
 *  - "ph_s" : base64 salt
 *  - "ph_h" : base64 PBKDF2 hash
 *  - "ph_i" : iteration count
 *  - "ph_v" : schema version (1)
 */
class PasscodeStore(context: Context) : PasscodeRepository {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (t: Throwable) {
        // Fail safely: surface the error to the user. We do NOT silently
        // downgrade to ordinary persistent preferences.
        Log.e(TAG, "Secure storage unavailable: ${t.javaClass.simpleName}")
        throw SecureStorageUnavailableException(t)
    }

    override fun isPasscodeSet(): Boolean =
        prefs.contains(KEY_HASH) && prefs.contains(KEY_SALT) && prefs.contains(KEY_ITER)

    override fun savePasscode(passcode: String): Boolean {
        return try {
            val hashed = PasscodeHasher.hash(passcode)
            prefs.edit()
                .putString(KEY_SALT, hashed.salt)
                .putString(KEY_HASH, hashed.hash)
                .putInt(KEY_ITER, hashed.iterations)
                .putInt(KEY_VERSION, SCHEMA_VERSION)
                .apply()
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to persist passcode: ${t.javaClass.simpleName}")
            false
        }
    }

    override fun verifyPasscode(passcode: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val hash = prefs.getString(KEY_HASH, null) ?: return false
        val iter = prefs.getInt(KEY_ITER, 0)
        if (iter <= 0) return false
        val stored = HashedPasscode(salt, hash, iter)
        return PasscodeHasher.verify(passcode, stored)
    }

    override fun changePasscode(
        currentPasscode: String,
        newPasscode: String
    ): ChangePasscodeResult {
        if (!verifyPasscode(currentPasscode)) return ChangePasscodeResult.IncorrectCurrent
        // savePasscode returns false only on storage failure, so the
        // two outcomes here are Success and StorageFailure.
        return if (savePasscode(newPasscode)) ChangePasscodeResult.Success
               else ChangePasscodeResult.StorageFailure
    }

    override fun removePasscode(currentPasscode: String): RemovePasscodeResult {
        if (!verifyPasscode(currentPasscode)) return RemovePasscodeResult.IncorrectCurrent
        return try {
            prefs.edit()
                .remove(KEY_SALT)
                .remove(KEY_HASH)
                .remove(KEY_ITER)
                .remove(KEY_VERSION)
                .apply()
            RemovePasscodeResult.Success
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to remove passcode: ${t.javaClass.simpleName}")
            RemovePasscodeResult.StorageFailure
        }
    }

    override fun forceRemove() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val TAG = "PasscodeStore"
        private const val FILE_NAME = "esp32_secure_lock_prefs"
        private const val KEY_SALT = "ph_s"
        private const val KEY_HASH = "ph_h"
        private const val KEY_ITER = "ph_i"
        private const val KEY_VERSION = "ph_v"
        private const val SCHEMA_VERSION = 1
    }
}

/**
 * Thrown when secure storage cannot be initialized. The UI layer turns
 * this into an actionable error message instead of silently degrading
 * to insecure persistence.
 */
class SecureStorageUnavailableException(cause: Throwable) :
    RuntimeException("Secure storage is unavailable on this device.", cause)

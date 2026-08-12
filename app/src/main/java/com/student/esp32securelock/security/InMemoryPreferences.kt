package com.student.esp32securelock.security

import android.content.SharedPreferences
import android.content.Context

/**
 * Fallback in-memory implementation of [SharedPreferences] used only when
 * [androidx.security.crypto.EncryptedSharedPreferences] cannot be created
 * (e.g. some emulator images). The passcode will not persist across process
 * restarts in that case, but the app will keep working without crashing.
 */
internal class InMemoryPreferences : SharedPreferences {

    private val data = linkedMapOf<String, Any?>()

    @Synchronized
    override fun getAll(): MutableMap<String, *> = data.toMutableMap()

    @Synchronized
    override fun getString(key: String?, defValue: String?): String? =
        data[key] as? String ?: defValue

    @Synchronized
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String> ?: defValues)

    @Synchronized
    override fun getInt(key: String?, defValue: Int): Int =
        data[key] as? Int ?: defValue

    @Synchronized
    override fun getLong(key: String?, defValue: Long): Long =
        data[key] as? Long ?: defValue

    @Synchronized
    override fun getFloat(key: String?, defValue: Float): Float =
        data[key] as? Float ?: defValue

    @Synchronized
    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        data[key] as? Boolean ?: defValue

    @Synchronized
    override fun contains(key: String?): Boolean = data.containsKey(key)

    @Synchronized
    override fun edit(): SharedPreferences.Editor = Editor()

    @Synchronized
    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
        // No-op
    }

    @Synchronized
    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
        // No-op
    }

    private inner class Editor : SharedPreferences.Editor {
        private val pending = linkedMapOf<String, Any?>()
        private val removes = mutableSetOf<String>()
        private var clear = false
        private var commitMode = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor =
            apply { pending[key] = value }

        override fun putStringSet(
            key: String,
            values: MutableSet<String>?
        ): SharedPreferences.Editor = apply { pending[key] = values }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor =
            apply { pending[key] = value }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor =
            apply { pending[key] = value }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor =
            apply { pending[key] = value }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor =
            apply { pending[key] = value }

        override fun remove(key: String): SharedPreferences.Editor = apply { removes.add(key) }

        override fun clear(): SharedPreferences.Editor = apply { clear = true }

        override fun commit(): Boolean { apply(); return true }

        override fun apply() {
            synchronized(this@InMemoryPreferences) {
                if (clear) data.clear()
                removes.forEach { data.remove(it) }
                pending.forEach { (k, v) ->
                    if (v == null) data.remove(k) else data[k] = v
                }
            }
        }
    }
}

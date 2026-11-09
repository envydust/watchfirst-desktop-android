package com.nuvio.app.features.access

import android.content.Context
import android.content.SharedPreferences

internal actual object WatchfirstAccessStorage {
    private const val PREFS_NAME = "watchfirst_access"
    private const val KEY_TOKEN = "session_token"
    private const val KEY_DEVICE = "device_id"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadToken(): String? = preferences?.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    actual fun saveToken(token: String?) {
        preferences?.edit()?.apply {
            if (token.isNullOrBlank()) remove(KEY_TOKEN) else putString(KEY_TOKEN, token)
        }?.apply()
    }

    actual fun loadDeviceId(): String? = preferences?.getString(KEY_DEVICE, null)

    actual fun saveDeviceId(deviceId: String) {
        preferences?.edit()?.putString(KEY_DEVICE, deviceId)?.apply()
    }

    actual fun loadName(): String? = preferences?.getString(KEY_NAME, null)

    actual fun loadEmail(): String? = preferences?.getString(KEY_EMAIL, null)

    actual fun saveIdentity(name: String, email: String) {
        preferences?.edit()?.putString(KEY_NAME, name)?.putString(KEY_EMAIL, email)?.apply()
    }

    actual fun loadValue(key: String): String? = preferences?.getString("extra_$key", null)

    actual fun saveValue(key: String, value: String?) {
        preferences?.edit()?.apply {
            if (value == null) remove("extra_$key") else putString("extra_$key", value)
        }?.apply()
    }
}

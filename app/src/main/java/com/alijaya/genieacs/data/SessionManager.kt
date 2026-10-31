package com.alijaya.genieacs.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("genieacs_mobile_session", Context.MODE_PRIVATE)

    companion object {
        const val KEY_SERVER_URL = "server_url"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_ROLE = "user_role" // "admin" or "customer"
        const val KEY_USER_IDENTIFIER = "user_identifier" // username or phone
        const val KEY_CUSTOMER_DEVICE_ID = "customer_device_id"
        const val KEY_AUTH_TOKEN = "auth_token"
        
        const val ROLE_ADMIN = "admin"
        const val ROLE_CUSTOMER = "customer"
        
        const val DEFAULT_SERVER = "https://server.alijaya.com"
        const val DEFAULT_SERVER_185 = "http://192.168.8.185:3000"
    }

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER) ?: DEFAULT_SERVER
        set(value) {
            val clean = value.trim().trimEnd('/')
            prefs.edit().putString(KEY_SERVER_URL, clean).apply()
        }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var userRole: String
        get() = prefs.getString(KEY_ROLE, ROLE_ADMIN) ?: ROLE_ADMIN
        set(value) = prefs.edit().putString(KEY_ROLE, value).apply()

    var userIdentifier: String
        get() = prefs.getString(KEY_USER_IDENTIFIER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_IDENTIFIER, value).apply()

    var customerDeviceId: String
        get() = prefs.getString(KEY_CUSTOMER_DEVICE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOMER_DEVICE_ID, value).apply()

    var authToken: String
        get() = prefs.getString(KEY_AUTH_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()

    fun saveAdminSession(token: String, username: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_ROLE, ROLE_ADMIN)
            .putString(KEY_USER_IDENTIFIER, username)
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
    }

    fun saveCustomerSession(phone: String, deviceId: String, token: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_ROLE, ROLE_CUSTOMER)
            .putString(KEY_USER_IDENTIFIER, phone)
            .putString(KEY_CUSTOMER_DEVICE_ID, deviceId)
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
    }

    fun logout() {
        val currentServer = serverUrl
        prefs.edit().clear().apply()
        serverUrl = currentServer
    }
}

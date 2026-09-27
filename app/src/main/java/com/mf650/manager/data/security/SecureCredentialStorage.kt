package com.mf650.manager.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure credential storage backed by Android Keystore (AES-256 GCM).
 * Protects router admin credentials and API tokens.
 */
class SecureCredentialStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "mf650_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getHost(): String = sharedPreferences.getString(KEY_HOST, "192.168.100.1") ?: "192.168.100.1"

    fun setHost(host: String) {
        sharedPreferences.edit().putString(KEY_HOST, host).apply()
    }

    fun getRouterIp(): String = getHost()

    fun saveRouterIp(host: String) = setHost(host)

    fun getAdminUsername(): String = sharedPreferences.getString(KEY_USER, "admin") ?: "admin"

    fun setAdminUsername(user: String) {
        sharedPreferences.edit().putString(KEY_USER, user).apply()
    }

    fun getAdminPassword(): String = sharedPreferences.getString(KEY_PASS, "admin") ?: "admin"

    fun setAdminPassword(pass: String) {
        sharedPreferences.edit().putString(KEY_PASS, pass).apply()
    }

    fun getCredentials(): Pair<String, String> = getAdminUsername() to getAdminPassword()

    companion object {
        private const val KEY_HOST = "router_host"
        private const val KEY_USER = "admin_user"
        private const val KEY_PASS = "admin_pass"
    }
}

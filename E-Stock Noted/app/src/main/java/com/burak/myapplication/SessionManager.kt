package com.burak.myapplication

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(user: UserModel, nowMillis: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_BUSINESS, user.business)
            .putString(KEY_ROLE, user.role.name)
            .putBoolean(KEY_MUST_CHANGE_PASSWORD, user.mustChangePassword)
            .putLong(KEY_LAST_ACTIVITY, nowMillis)
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun isSessionExpired(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!isLoggedIn()) return true
        val lastActivity = prefs.getLong(KEY_LAST_ACTIVITY, 0L)
        if (lastActivity <= 0L) return false
        return (nowMillis - lastActivity) > SESSION_TIMEOUT_MILLIS
    }

    fun touchActivity(nowMillis: Long = System.currentTimeMillis()) {
        if (isLoggedIn()) {
            prefs.edit().putLong(KEY_LAST_ACTIVITY, nowMillis).apply()
        }
    }

    fun markPasswordChanged() {
        prefs.edit().putBoolean(KEY_MUST_CHANGE_PASSWORD, false).apply()
    }

    fun mustChangePassword(): Boolean = prefs.getBoolean(KEY_MUST_CHANGE_PASSWORD, false)

    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""

    fun getBusiness(): String = prefs.getString(KEY_BUSINESS, "") ?: ""

    fun getRole(): UserRole = UserRole.fromString(prefs.getString(KEY_ROLE, UserRole.USER.name))

    fun isAdmin(): Boolean = isLoggedIn() && getRole().canManageUsers

    fun canManageStock(): Boolean = isLoggedIn() && getRole().canManageStock

    companion object {
        const val SESSION_TIMEOUT_MILLIS = 15 * 60 * 1000L // 15 minutes
        private const val PREFS_NAME = "e_stock_noted_session"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_BUSINESS = "business"
        private const val KEY_ROLE = "role"
        private const val KEY_MUST_CHANGE_PASSWORD = "must_change_password"
        private const val KEY_LAST_ACTIVITY = "last_activity"
    }
}

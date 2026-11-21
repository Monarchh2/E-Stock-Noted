package com.burak.myapplication

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(user: UserModel) {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_BUSINESS, user.business)
            .putString(KEY_ROLE, user.role.name)
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""

    fun getBusiness(): String = prefs.getString(KEY_BUSINESS, "") ?: ""

    fun getRole(): UserRole = UserRole.fromString(prefs.getString(KEY_ROLE, UserRole.USER.name))

    fun isAdmin(): Boolean = isLoggedIn() && getRole() == UserRole.ADMIN

    companion object {
        private const val PREFS_NAME = "e_stock_noted_session"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_BUSINESS = "business"
        private const val KEY_ROLE = "role"
    }
}

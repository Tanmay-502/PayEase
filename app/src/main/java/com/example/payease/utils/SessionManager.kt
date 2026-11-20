package com.example.payease.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "PayEaseSession"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_ID = "userId"
        private const val KEY_USER_NAME = "userName"
        private const val KEY_USER_EMAIL = "userEmail"
        private const val KEY_USER_MOBILE = "userMobile"
        private const val KEY_IS_BALANCE_VISIBLE = "isBalanceVisible"
    }

    fun createLoginSession(userId: Long, fullName: String, email: String, mobile: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, fullName)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_MOBILE, mobile)
            apply()
        }
    }

    val isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    val userId: Long
        get() = prefs.getLong(KEY_USER_ID, -1L)

    val userName: String
        get() = prefs.getString(KEY_USER_NAME, "User") ?: "User"

    val userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "") ?: ""

    val userMobile: String
        get() = prefs.getString(KEY_USER_MOBILE, "") ?: ""

    var isBalanceVisible: Boolean
        get() = prefs.getBoolean(KEY_IS_BALANCE_VISIBLE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_IS_BALANCE_VISIBLE, value).apply()
        }

    fun logout() {
        prefs.edit().apply {
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_MOBILE)
            apply()
        }
    }
}

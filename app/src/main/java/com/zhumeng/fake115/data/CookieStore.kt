package com.zhumeng.fake115.data

import android.content.Context

object CookieStore {
    private const val PREFS_NAME = "auth_115_prefs"
    private const val KEY_COOKIE = "cookie_115"

    fun getCookie(context: Context): String {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_COOKIE, "")
            .orEmpty()
            .trim()
    }

    fun saveCookie(context: Context, cookie: String) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COOKIE, cookie.trim())
            .apply()
    }

    fun requireCookie(context: Context): String {
        return getCookie(context).ifBlank {
            throw IllegalStateException("未配置 115 Cookie，请先在设置页保存。")
        }
    }
}

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
            throw IllegalStateException("\u672a\u914d\u7f6e 115 Cookie\uff0c\u8bf7\u5148\u5728\u8bbe\u7f6e\u9875\u4fdd\u5b58\u3002")
        }
    }
}

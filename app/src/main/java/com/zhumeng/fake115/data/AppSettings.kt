package com.zhumeng.fake115.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppSettings {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_QUICK_MANAGEMENT = "quick_management"

    private val quickManagementEnabled = MutableStateFlow(false)
    private var initialized = false

    fun quickManagementEnabledFlow(context: Context): StateFlow<Boolean> {
        ensureInitialized(context)
        return quickManagementEnabled.asStateFlow()
    }

    fun isQuickManagementEnabled(context: Context): Boolean {
        ensureInitialized(context)
        return quickManagementEnabled.value
    }

    fun setQuickManagementEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_QUICK_MANAGEMENT, enabled)
            .apply()
        quickManagementEnabled.value = enabled
    }

    private fun ensureInitialized(context: Context) {
        if (initialized) return
        quickManagementEnabled.value = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_QUICK_MANAGEMENT, false)
        initialized = true
    }
}

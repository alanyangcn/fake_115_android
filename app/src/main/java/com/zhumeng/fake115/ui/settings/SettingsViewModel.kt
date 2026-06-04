package com.zhumeng.fake115.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhumeng.fake115.data.ApiConfig
import com.zhumeng.fake115.data.AppSettings
import com.zhumeng.fake115.data.CookieStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class SettingsUiState(
    val cookieInput: String = "",
    val isFetching: Boolean = false,
    val message: String? = null,
    val hasSavedCookie: Boolean = false,
    val quickManagementEnabled: Boolean = false,
)

class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(
        SettingsUiState().copyFromSavedCookie(
            CookieStore.getCookie(application)
        ).copy(
            quickManagementEnabled = AppSettings.isQuickManagementEnabled(application),
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onCookieChanged(value: String) {
        _uiState.update {
            it.copy(
                cookieInput = value,
                message = null,
            )
        }
    }

    fun saveCookie() {
        val cookie = _uiState.value.cookieInput.trim()
        CookieStore.saveCookie(getApplication(), cookie)
        _uiState.update {
            it.copy(
                cookieInput = cookie,
                hasSavedCookie = cookie.isNotBlank(),
                message = if (cookie.isBlank()) {
                    "Cookie 已清空。"
                } else {
                    "Cookie 已保存到本地。"
                },
            )
        }
    }

    fun setQuickManagementEnabled(enabled: Boolean) {
        AppSettings.setQuickManagementEnabled(getApplication(), enabled)
        _uiState.update {
            it.copy(quickManagementEnabled = enabled)
        }
    }

    fun fetchCookie() {
        if (_uiState.value.isFetching) return

        viewModelScope.launch {
            _uiState.update { it.copy(isFetching = true, message = null) }

            runCatching { fetchCookieFromServer() }
                .onSuccess { cookie ->
                    _uiState.update {
                        it.copy(
                            cookieInput = cookie,
                            isFetching = false,
                            message = "已成功获取 Cookie，请点击保存。",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isFetching = false,
                            message = error.message ?: "获取 Cookie 失败。",
                        )
                    }
                }
        }
    }

    private suspend fun fetchCookieFromServer(): String = withContext(Dispatchers.IO) {
        val endpoint = "${ApiConfig.BASE_URL}/api/auth115/cookie"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
        }

        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            }.orEmpty()

            if (code !in 200..299) {
                throw IllegalStateException(body.ifBlank { "请求失败，HTTP $code。" })
            }

            val json = JSONObject(body)
            if (!json.optBoolean("success")) {
                throw IllegalStateException(
                    json.optString("message").ifBlank { "获取 Cookie 失败。" }
                )
            }
            if (!json.optBoolean("configured")) {
                throw IllegalStateException("服务端还没有配置 Cookie。")
            }

            json.optString("cookie")
                .trim()
                .ifBlank { throw IllegalStateException("返回的 Cookie 为空。") }
        } finally {
            connection.disconnect()
        }
    }

    private fun SettingsUiState.copyFromSavedCookie(savedCookie: String): SettingsUiState {
        return copy(
            cookieInput = savedCookie,
            hasSavedCookie = savedCookie.isNotBlank(),
        )
    }
}

package com.zhumeng.fake115.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhumeng.fake115.data.ApiConfig
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
)

class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(
        SettingsUiState().copyFromSavedCookie(
            CookieStore.getCookie(application)
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
                    "Cookie \u5df2\u6e05\u7a7a\u3002"
                } else {
                    "Cookie \u5df2\u4fdd\u5b58\u5230\u672c\u5730\u3002"
                },
            )
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
                            message = "\u5df2\u6210\u529f\u83b7\u53d6 Cookie\uff0c\u8bf7\u70b9\u51fb\u4fdd\u5b58\u3002",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isFetching = false,
                            message = error.message ?: "\u83b7\u53d6 Cookie \u5931\u8d25\u3002",
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
                throw IllegalStateException(body.ifBlank { "\u8bf7\u6c42\u5931\u8d25\uff0cHTTP $code\u3002" })
            }

            val json = JSONObject(body)
            if (!json.optBoolean("success")) {
                throw IllegalStateException(
                    json.optString("message").ifBlank { "\u83b7\u53d6 Cookie \u5931\u8d25\u3002" }
                )
            }
            if (!json.optBoolean("configured")) {
                throw IllegalStateException("\u670d\u52a1\u7aef\u8fd8\u6ca1\u6709\u914d\u7f6e Cookie\u3002")
            }

            json.optString("cookie")
                .trim()
                .ifBlank { throw IllegalStateException("\u8fd4\u56de\u7684 Cookie \u4e3a\u7a7a\u3002") }
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

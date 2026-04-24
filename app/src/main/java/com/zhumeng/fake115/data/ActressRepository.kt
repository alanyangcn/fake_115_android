package com.zhumeng.fake115.data

import android.net.Uri
import android.util.Log
import com.zhumeng.fake115.data.model.Actress
import com.zhumeng.fake115.data.model.ActressQuery
import com.zhumeng.fake115.data.model.ActressResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.net.toUri

class ActressRepository(
    private val baseUrl: String = ApiConfig.BASE_URL,
) {
    suspend fun fetchActresses(query: ActressQuery): ActressResponse = withContext(Dispatchers.IO) {
        val uri = buildUri("$baseUrl/api/actresses", query.toMap())
        val json = requestJson(uri)

        ActressResponse(
            actresses = parseActresses(json.optJSONArray("actresses")),
            total = json.optInt("total"),
            page = json.optInt("page", query.page),
            limit = json.optInt("limit", query.limit),
        )
    }

    private fun buildUri(base: String, params: Map<String, String>): Uri {
        val builder = base.toUri().buildUpon()
        params.forEach { (key, value) -> builder.appendQueryParameter(key, value) }
        return builder.build()
    }

    private fun requestJson(uri: Uri): JSONObject {
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
        }

        return try {
            Log.d(TAG, "Request: GET ${uri}")
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            }.orEmpty()
            Log.d(TAG, "Response[$code]: ${body.take(LOG_PREVIEW_LENGTH)}")

            if (code !in 200..299) {
                throw IllegalStateException(body.ifBlank { "Request failed with HTTP $code." })
            }

            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseActresses(rows: JSONArray?): List<Actress> {
        if (rows == null) return emptyList()

        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                add(
                    Actress(
                        id = row.optInt("id"),
                        name = row.optString("name"),
                        avatar = row.optString("avatar").takeIf { it.isNotBlank() && it != "null" },
                        videoCount = row.optInt("videoCount"),
                    )
                )
            }
        }
    }

    companion object {
        private const val TAG = "ActressRepository"
        private const val LOG_PREVIEW_LENGTH = 4000
    }
}

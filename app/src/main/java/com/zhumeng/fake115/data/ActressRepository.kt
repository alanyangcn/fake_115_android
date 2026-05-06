package com.zhumeng.fake115.data

import android.net.Uri
import android.util.Log
import com.zhumeng.fake115.data.model.Actress
import com.zhumeng.fake115.data.model.ActressQuery
import com.zhumeng.fake115.data.model.ActressResponse
import com.zhumeng.fake115.data.model.ActressVideosFavoriteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
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

    suspend fun updateFavorite(actressId: Int, favorite: Boolean): Boolean = withContext(Dispatchers.IO) {
        val uri = buildUri("$baseUrl/api/actresses/$actressId/favorite", emptyMap())
        val json = requestJson(
            uri = uri,
            method = "POST",
            body = JSONObject().put("favorite", favorite).toString(),
        )
        json.optBoolean("favorite", favorite)
    }

    suspend fun updateVideosFavorite(actressId: Int, favorite: Boolean): ActressVideosFavoriteResult =
        withContext(Dispatchers.IO) {
            val uri = buildUri("$baseUrl/api/actresses/$actressId/favorite-videos", emptyMap())
            val json = requestJson(
                uri = uri,
                method = "POST",
                body = JSONObject().put("favorite", favorite).toString(),
            )
            ActressVideosFavoriteResult(
                favorite = json.optBoolean("favorite", favorite),
                count = json.optInt("count", 0),
                deletedMissingCount = json.optInt("deletedMissingCount", 0),
            )
        }

    private fun buildUri(base: String, params: Map<String, String>): Uri {
        val builder = base.toUri().buildUpon()
        params.forEach { (key, value) -> builder.appendQueryParameter(key, value) }
        return builder.build()
    }

    private fun requestJson(
        uri: Uri,
        method: String = "GET",
        body: String? = null,
    ): JSONObject {
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        return try {
            Log.d(
                TAG,
                buildString {
                    append("Request: ")
                    append(method)
                    append(' ')
                    append(uri)
                    if (body != null) {
                        append(" body=")
                        append(body.take(LOG_PREVIEW_LENGTH))
                    }
                }
            )
            if (body != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body)
                }
            }
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
                        isFavorite = row.optInt("isFavorite"),
                        favoriteAt = row.optLong("favoriteAt").takeIf { it > 0L },
                        isFavoriteAllVideos = row.optInt("isFavoriteAllVideos"),
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

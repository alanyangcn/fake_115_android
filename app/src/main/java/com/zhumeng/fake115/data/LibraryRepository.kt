package com.zhumeng.fake115.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.zhumeng.fake115.data.model.GenreOption
import com.zhumeng.fake115.data.model.LibraryMovie
import com.zhumeng.fake115.data.model.LibraryQuery
import com.zhumeng.fake115.data.model.LibraryResponse
import com.zhumeng.fake115.data.model.VideoDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class LibraryRepository(
    private val context: Context,
    private val baseUrl: String = ApiConfig.BASE_URL,
) {
    fun build115RequestHeaders(): Map<String, String> {
        val cookie = CookieStore.getCookie(context)
        return buildMap {
            put("accept", "*/*")
            put("accept-language", "zh-CN,zh;q=0.9")
            put("user-agent", ApiConfig.USER_AGENT_115)
            if (cookie.isNotBlank()) {
                put("cookie", cookie)
            }
        }
    }

    suspend fun fetchLibrary(query: LibraryQuery): LibraryResponse = withContext(Dispatchers.IO) {
        val uri = buildUri("$baseUrl/api/videos/library", query.toMap())
        val json = requestJson(uri)

        LibraryResponse(
            videos = parseVideos(json.optJSONArray("videos")),
            total = json.optInt("total"),
            page = json.optInt("page", query.page),
            limit = json.optInt("limit", query.limit),
        )
    }

    suspend fun fetchGenres(): List<GenreOption> = withContext(Dispatchers.IO) {
        val uri = buildUri("$baseUrl/api/genres", emptyMap())
        val json = requestJson(uri)
        val rows = json.optJSONArray("genres") ?: JSONArray()

        buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                add(
                    GenreOption(
                        id = row.optInt("id"),
                        name = row.optString("name"),
                        videoCount = row.optInt("videoCount", 0),
                    )
                )
            }
        }
    }

    suspend fun fetchVideoDetail(videoId: Int): VideoDetail = withContext(Dispatchers.IO) {
        val uri = buildUri("$baseUrl/api/videos/$videoId", emptyMap())
        parseVideoDetail(requestJson(uri))
    }

    suspend fun updateFavorite(movieId: Int, favorite: Boolean): Boolean = withContext(Dispatchers.IO) {
        val uri = buildUri("$baseUrl/api/videos/$movieId/favorite", emptyMap())
        val json = requestJson(
            uri = uri,
            method = "POST",
            body = JSONObject().put("favorite", favorite).toString(),
        )
        json.optBoolean("favorite", favorite)
    }

    suspend fun deleteMovie(movieId: Int): String = withContext(Dispatchers.IO) {
        val uri = buildUri("$baseUrl/api/videos/$movieId", emptyMap())
        val json = requestJson(
            uri = uri,
            method = "DELETE",
        )
        deletedMovieEventsInternal.tryEmit(movieId)
        json.optString("message").ifBlank { "删除成功" }
    }

    fun buildProxyUrl(pc: String): String {
        return buildUri("$baseUrl/api/proxy/v", mapOf("pc" to pc)).toString()
    }

    suspend fun resolve115PlayableUrl(pc: String): String = withContext(Dispatchers.IO) {
        val cookie = CookieStore.requireCookie(context)
        val playlistUrl = "https://115.com/api/video/m3u8/${Uri.encode(pc)}.m3u8"
        val connection = (URL(playlistUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            buildMap {
                putAll(build115RequestHeaders())
                put("cookie", cookie)
            }.forEach { (key, value) ->
                setRequestProperty(key, value)
            }
        }

        try {
            Log.d(TAG, "Request: GET $playlistUrl")
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            }.orEmpty()
            Log.d(TAG, "Response[$code]: ${body.take(LOG_PREVIEW_LENGTH)}")

            if (code !in 200..299) {
                throw IllegalStateException(body.ifBlank { "Failed to fetch 115 playlist: HTTP $code" })
            }

            body.lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotBlank() && !it.startsWith("#") && it.startsWith("http") }
                ?: throw IllegalStateException("115 playlist did not contain a playable m3u8 url.")
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUri(base: String, params: Map<String, String>): Uri {
        val builder = Uri.parse(base).buildUpon()
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
            val responseBody = stream?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            }.orEmpty()
            Log.d(TAG, "Response[$code]: ${responseBody.take(LOG_PREVIEW_LENGTH)}")

            if (code !in 200..299) {
                throw IllegalStateException(responseBody.ifBlank { "Request failed with HTTP $code." })
            }

            JSONObject(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseVideos(rows: JSONArray?): List<LibraryMovie> {
        if (rows == null) return emptyList()

        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                add(
                    LibraryMovie(
                        id = row.optInt("id"),
                        fid = row.optString("fid"),
                        pc = row.optString("pc"),
                        fanhao = row.optString("fanhao"),
                        name = row.optString("name"),
                        cover = row.optString("cover").takeIf { it.isNotBlank() && it != "null" },
                        actressIds = row.optString("actressIds").takeIf { it.isNotBlank() && it != "null" },
                        isFavorite = row.optInt("isFavorite"),
                        favoriteAt = row.optLong("favoriteAt").takeIf { it > 0L },
                    )
                )
            }
        }
    }

    private fun parseVideoDetail(json: JSONObject): VideoDetail {
        return VideoDetail(
            id = json.optInt("id"),
            fid = json.optString("fid"),
            fileName = json.optString("fileName"),
            pc = json.optString("pc"),
            fanhao = json.optString("fanhao"),
            name = json.optString("name"),
            size = json.optLong("size"),
            uploadDate = json.optLong("uploadDate"),
            releaseDate = json.optString("releaseDate").takeIf { it.isNotBlank() && it != "null" },
            duration = json.optInt("duration"),
            cover = json.optString("cover").takeIf { it.isNotBlank() && it != "null" },
            isFavorite = json.optInt("isFavorite"),
            favoriteAt = json.optLong("favoriteAt").takeIf { it > 0L },
            createdAt = json.optLong("createdAt"),
            studio = json.optString("studio").takeIf { it.isNotBlank() && it != "null" },
            publisher = json.optString("publisher").takeIf { it.isNotBlank() && it != "null" },
            series = json.optString("series").takeIf { it.isNotBlank() && it != "null" },
            genres = parseStringList(json.optJSONArray("genres")),
            actresses = parseStringList(json.optJSONArray("actresses")),
        )
    }

    private fun parseStringList(rows: JSONArray?): List<String> {
        if (rows == null) return emptyList()
        return buildList {
            for (index in 0 until rows.length()) {
                rows.optString(index)
                    .takeIf { it.isNotBlank() && it != "null" }
                    ?.let(::add)
            }
        }
    }

    companion object {
        private const val TAG = "LibraryRepository"
        private const val LOG_PREVIEW_LENGTH = 4000
        private val deletedMovieEventsInternal = MutableSharedFlow<Int>(extraBufferCapacity = 8)

        val deletedMovieEvents: SharedFlow<Int> = deletedMovieEventsInternal.asSharedFlow()
    }
}

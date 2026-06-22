package com.zhumeng.fake115.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.zhumeng.fake115.data.model.NetDiskFile
import com.zhumeng.fake115.data.model.NetDiskDetailPathNode
import com.zhumeng.fake115.data.model.NetDiskFileDetail
import com.zhumeng.fake115.data.model.NetDiskFileLabel
import com.zhumeng.fake115.data.model.NetDiskPathNode
import com.zhumeng.fake115.data.model.NetDiskQuery
import com.zhumeng.fake115.data.model.NetDiskResponse
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

private const val NET_DISK_LOAD_ERROR = "加载网盘文件失败。"
private const val DEFAULT_FOLDER_NAME = "目录"

class NetDiskRepository(
    private val context: Context,
    private val baseUrl: String = "https://webapi.115.com/files",
) {
    suspend fun fetchFiles(query: NetDiskQuery): NetDiskResponse = withContext(Dispatchers.IO) {
        val uri = buildUri(baseUrl, query.toMap())
        val json = requestJson(uri)

        if (!json.optBoolean("state", false)) {
            throw IllegalStateException(json.optString("message").ifBlank { NET_DISK_LOAD_ERROR })
        }

        NetDiskResponse(
            files = parseFiles(json.optJSONArray("data")),
            count = json.optInt("count"),
            offset = json.optInt("offset", query.offset),
            limit = json.optInt("limit", query.limit),
            cid = json.opt("cid")?.toString().orEmpty(),
            path = parsePath(json.optJSONArray("path")),
        )
    }

    suspend fun fetchFileDetail(
        id: String,
        isDirectory: Boolean,
    ): NetDiskFileDetail = withContext(Dispatchers.IO) {
        val uri = Uri.parse("https://webapi.115.com/category/get")
            .buildUpon()
            .appendQueryParameter(if (isDirectory) "cid" else "fid", id)
            .appendQueryParameter("status", "1")
            .build()
        val json = requestJson(uri)

        if (!json.optBoolean("state", false)) {
            throw IllegalStateException(json.optString("error").ifBlank { NET_DISK_LOAD_ERROR })
        }

        parseFileDetail(json, id, isDirectory)
    }

    suspend fun updateStar(
        fileId: String,
        star: Boolean,
    ): Boolean = withContext(Dispatchers.IO) {
        val json = requestJson(
            uri = Uri.parse("https://webapi.115.com/files/star"),
            method = "POST",
            formParams = linkedMapOf(
                "file_id" to fileId,
                "star" to if (star) "1" else "0",
                "format" to "json",
            ),
        )

        if (!json.optBoolean("state", false)) {
            throw IllegalStateException(json.optString("error").ifBlank { NET_DISK_LOAD_ERROR })
        }

        starredFileEventsInternal.tryEmit(StarredFileEvent(fileId = fileId, isStarred = star))
        star
    }

    suspend fun deleteFile(fileId: String): String = withContext(Dispatchers.IO) {
        val json = requestJson(
            uri = Uri.parse("https://webapi.115.com/rb/delete"),
            method = "POST",
            formParams = linkedMapOf(
                "fid[0]" to fileId,
            ),
        )

        if (!json.optBoolean("state", false)) {
            throw IllegalStateException(json.optString("error").ifBlank { "删除失败。" })
        }

        return@withContext "删除成功"
    }

    suspend fun moveFile(
        fileId: String,
        targetCid: String,
    ): String = withContext(Dispatchers.IO) {
        val json = requestJson(
            uri = Uri.parse("https://webapi.115.com/files/move"),
            method = "POST",
            formParams = linkedMapOf(
                "pid" to targetCid,
                "move_proid" to "${System.currentTimeMillis()}_-63_0",
                "fid[0]" to fileId,
            ),
        )

        if (!json.optBoolean("state", false)) {
            throw IllegalStateException(
                json.optString("error")
                    .ifBlank { json.optString("message") }
                    .ifBlank { "移动失败。" }
            )
        }

        return@withContext json.optString("message").ifBlank { "移动成功" }
    }

    private fun buildUri(base: String, params: Map<String, String>): Uri {
        val builder = Uri.parse(base).buildUpon()
        params.forEach { (key, value) -> builder.appendQueryParameter(key, value) }
        return builder.build()
    }

    private fun requestJson(
        uri: Uri,
        method: String = "GET",
        formParams: Map<String, String>? = null,
    ): JSONObject {
        val cookie = CookieStore.requireCookie(context)
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", ApiConfig.USER_AGENT_115)
            setRequestProperty("Cookie", cookie)
            setRequestProperty("Referer", "https://115.com/")
            if (!formParams.isNullOrEmpty()) {
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            }
        }

        return try {
            val encodedForm = formParams?.toFormBody()
            Log.d(TAG, "Request: $method $uri${encodedForm?.let { " body=$it" } ?: ""}")
            if (!encodedForm.isNullOrBlank()) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(encodedForm)
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

    private fun parseFiles(rows: JSONArray?): List<NetDiskFile> {
        if (rows == null) return emptyList()

        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val isDirectory = row.optInt("fc", -1) == 0 || row.optString("fc") == "0"
                val itemId = if (isDirectory) {
                    row.opt("cid")?.toString().orEmpty()
                } else {
                    row.opt("fid")?.toString().orEmpty()
                }
                val parentId = if (isDirectory) {
                    row.opt("pid")?.toString().orEmpty()
                } else {
                    row.opt("cid")?.toString().orEmpty()
                }
                val suffix = row.optMeaningfulString("ico") ?: row.optMeaningfulString("class")
                val updateTime = row.optEpoch("te") ?: row.optEpoch("t")
                val uploadTime = row.optEpoch("tp") ?: row.optEpoch("tu")

                add(
                    NetDiskFile(
                        id = itemId.ifBlank { "${parentId}_${row.optMeaningfulString("n") ?: DEFAULT_FOLDER_NAME}" },
                        parentId = parentId,
                        isDirectory = isDirectory,
                        n = row.optMeaningfulString("n") ?: DEFAULT_FOLDER_NAME,
                        ns = row.optMeaningfulString("ns"),
                        remark = row.optMeaningfulString("fdesc"),
                        size = row.optLong("s"),
                        updateTime = updateTime,
                        uploadTime = uploadTime,
                        durationSeconds = row.optLong("play_long").takeIf { it > 0L },
                        fileType = row.optMeaningfulString("class") ?: suffix,
                        suffix = suffix,
                        isStarred = row.optLong("star_time") > 0L || row.optInt("m") == 1,
                        isEncrypted = row.optInt("p") == 1,
                        isVideo = row.optInt("iv") == 1 || row.optLong("play_long") > 0L,
                        thumbnail = row.optMeaningfulString("u"),
                        pc = row.optMeaningfulString("pc"),
                    )
                )
            }
        }
    }

    private fun parsePath(rows: JSONArray?): List<NetDiskPathNode> {
        if (rows == null) return emptyList()

        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                add(
                    NetDiskPathNode(
                        cid = row.opt("cid")?.toString().orEmpty(),
                        pid = row.opt("pid")?.toString(),
                        name = row.optString("name").ifBlank { DEFAULT_FOLDER_NAME },
                    )
                )
            }
        }
    }

    private fun parseFileDetail(
        json: JSONObject,
        id: String,
        isDirectory: Boolean,
    ): NetDiskFileDetail {
        return NetDiskFileDetail(
            id = id,
            isDirectory = isDirectory,
            fileName = json.optMeaningfulString("file_name") ?: DEFAULT_FOLDER_NAME,
            pickCode = json.optMeaningfulString("pick_code").orEmpty(),
            sha1 = json.optMeaningfulString("sha1").orEmpty(),
            size = json.opt("size")?.toString().orEmpty(),
            count = json.opt("count")?.toString().orEmpty(),
            folderCount = json.opt("folder_count")?.toString().orEmpty(),
            playLongSeconds = json.optLong("play_long"),
            showPlayLongSeconds = json.optLong("show_play_long"),
            createTime = json.optEpoch("ctime"),
            updateTime = json.optEpoch("utime"),
            uploadTime = json.optEpoch("ptime"),
            openTime = json.optEpoch("open_time"),
            isShare = json.opt("is_share")?.toString() == "1",
            isPrivate = json.optInt("is_private") == 1,
            isMarked = json.opt("is_mark")?.toString() == "1",
            score = json.opt("score")?.toString().orEmpty(),
            desc = json.optMeaningfulString("desc").orEmpty(),
            fileCategory = json.opt("file_category")?.toString().orEmpty(),
            labels = parseFileLabels(json.optJSONArray("fl")),
            paths = parseDetailPath(json.optJSONArray("paths")),
        )
    }

    private fun parseFileLabels(rows: JSONArray?): List<NetDiskFileLabel> {
        if (rows == null) return emptyList()

        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val name = row.optMeaningfulString("name") ?: continue
                add(
                    NetDiskFileLabel(
                        id = row.opt("id")?.toString().orEmpty(),
                        name = name,
                        sort = row.opt("sort")?.toString().orEmpty(),
                        color = row.optMeaningfulString("color").orEmpty(),
                        updateTime = row.optEpoch("update_time"),
                        createTime = row.optEpoch("create_time"),
                    )
                )
            }
        }
    }

    private fun parseDetailPath(rows: JSONArray?): List<NetDiskDetailPathNode> {
        if (rows == null) return emptyList()

        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                add(
                    NetDiskDetailPathNode(
                        fileId = row.opt("file_id")?.toString().orEmpty(),
                        fileName = row.optString("file_name").ifBlank { DEFAULT_FOLDER_NAME },
                    )
                )
            }
        }
    }

    companion object {
        const val CLASSIFIED_TARGET_CID = "3453630900415081379"

        private const val TAG = "NetDiskRepository"
        private const val LOG_PREVIEW_LENGTH = 4000
        private val starredFileEventsInternal = MutableSharedFlow<StarredFileEvent>(extraBufferCapacity = 16)
        private val deletedFileEventsInternal = MutableSharedFlow<String>(extraBufferCapacity = 16)

        val starredFileEvents: SharedFlow<StarredFileEvent> = starredFileEventsInternal.asSharedFlow()
        val deletedFileEvents: SharedFlow<String> = deletedFileEventsInternal.asSharedFlow()

        fun notifyFileDeleted(fileId: String) {
            deletedFileEventsInternal.tryEmit(fileId)
        }
    }
}

data class StarredFileEvent(
    val fileId: String,
    val isStarred: Boolean,
)

private fun JSONObject.optMeaningfulString(key: String): String? {
    val value = opt(key)?.toString()?.trim().orEmpty()
    return value.takeIf { it.isNotEmpty() && it != "null" }
}

private fun JSONObject.optEpoch(key: String): Long? {
    val raw = opt(key)?.toString()?.trim().orEmpty()
    if (raw.isEmpty() || raw == "null" || raw == "0") return null
    return raw.toLongOrNull()
}

private fun Map<String, String>.toFormBody(): String {
    val builder = Uri.Builder()
    forEach { (key, value) ->
        builder.appendQueryParameter(key, value)
    }
    return builder.build().encodedQuery.orEmpty()
}

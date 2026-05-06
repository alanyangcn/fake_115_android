package com.zhumeng.fake115.data.model

data class NetDiskFile(
    val id: String,
    val parentId: String,
    val isDirectory: Boolean,
    val n: String,
    val ns: String?,
    val remark: String?,
    val size: Long,
    val updateTime: Long?,
    val uploadTime: Long?,
    val durationSeconds: Long?,
    val fileType: String?,
    val suffix: String?,
    val isStarred: Boolean,
    val isEncrypted: Boolean,
    val isVideo: Boolean,
    val thumbnail: String?,
    val pc: String?,
) {
    val name: String
        get() = n
}

data class NetDiskPathNode(
    val cid: String,
    val pid: String?,
    val name: String,
)

data class NetDiskResponse(
    val files: List<NetDiskFile>,
    val count: Int,
    val offset: Int,
    val limit: Int,
    val cid: String,
    val path: List<NetDiskPathNode>,
)

data class NetDiskQuery(
    val aid: String = "1",
    val cid: String? = null,
    val type: String? = null,
    val limit: Int = 50,
    val offset: Int = 0,
    val suffix: String? = null,
    val asc: Int = 0,
    val orderBy: String = "file_size",
    val customOrder: String = "1",
    val star: String = "",
    val showDir: Int = 1,
    val code: String = "",
    val scid: String = "",
    val snap: Int = 0,
    val natsort: Int = 1,
    val recordOpenTime: Int = 1,
    val countFolders: Int = 1,
    val source: String = "",
    val format: String = "json",
    val isShare: String = "",
    val fcMix: String = "",
    val isQ: Int = 0,
) {
    fun toMap(): Map<String, String> {
        return buildMap {
            put("aid", aid)
            cid?.takeIf { it.isNotBlank() }?.let { put("cid", it) }
            put("o", orderBy)
            put("asc", asc.toString())
            put("limit", limit.toString())
            put("offset", offset.toString())
            put("show_dir", showDir.toString())
            put("code", code)
            put("scid", scid)
            put("snap", snap.toString())
            put("natsort", natsort.toString())
            put("record_open_time", recordOpenTime.toString())
            put("count_folders", countFolders.toString())
            put("type", type ?: "")
            put("source", source)
            put("format", format)
            put("star", star)
            put("is_share", isShare)
            suffix?.takeIf { it.isNotBlank() }?.let { put("suffix", it) } ?: put("suffix", "")
            put("custom_order", customOrder)
            put("fc_mix", fcMix)
            put("is_q", "")
        }
    }
}

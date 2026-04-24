package com.zhumeng.fake115.data.model

data class Actress(
    val id: Int,
    val name: String,
    val avatar: String?,
    val videoCount: Int,
)

data class ActressResponse(
    val actresses: List<Actress>,
    val total: Int,
    val page: Int,
    val limit: Int,
)

data class ActressQuery(
    val page: Int = 1,
    val limit: Int = 115,
    val search: String = "",
) {
    fun toMap(): Map<String, String> {
        return buildMap {
            put("page", page.toString())
            put("limit", limit.toString())
            if (search.isNotBlank()) put("search", search)
        }
    }
}

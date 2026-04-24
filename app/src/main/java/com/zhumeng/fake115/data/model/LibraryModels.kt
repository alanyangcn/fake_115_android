package com.zhumeng.fake115.data.model

data class LibraryMovie(
    val id: Int,
    val fid: String,
    val pc: String,
    val fanhao: String,
    val name: String,
    val cover: String?,
    val actressIds: String?,
    val isFavorite: Int,
    val favoriteAt: Long?,
)

data class GenreOption(
    val id: Int,
    val name: String,
    val videoCount: Int,
)

data class LibraryResponse(
    val videos: List<LibraryMovie>,
    val total: Int,
    val page: Int,
    val limit: Int,
)

data class VideoDetail(
    val id: Int,
    val fid: String,
    val fileName: String,
    val pc: String,
    val fanhao: String,
    val name: String,
    val size: Long,
    val uploadDate: Long,
    val releaseDate: String?,
    val duration: Int,
    val cover: String?,
    val isFavorite: Int,
    val favoriteAt: Long?,
    val createdAt: Long,
    val studio: String?,
    val publisher: String?,
    val series: String?,
    val genres: List<String>,
    val actresses: List<String>,
)

enum class FavoriteFilterMode {
    All,
    Favorite,
    Unfavorite,
}

enum class ViewMode {
    Normal,
    Compact,
}

enum class SortOption(val queryValue: String, val label: String) {
    ReleaseDate("releaseDate", "Release Date"),
    CreatedAt("createdAt", "Added Time"),
    FavoriteAt("favoriteAt", "Favorite Time"),
    Fanhao("fanhao", "Code"),
}

data class LibraryQuery(
    val page: Int = 1,
    val limit: Int = 24,
    val search: String = "",
    val sortBy: String = SortOption.ReleaseDate.queryValue,
    val sortOrder: String = "desc",
    val favorite: String? = null,
    val year: String? = null,
    val month: String? = null,
    val genres: String? = null,
) {
    fun toMap(): Map<String, String> {
        return buildMap {
            put("page", page.toString())
            put("limit", limit.toString())
            if (search.isNotBlank()) put("search", search)
            put("sortBy", sortBy)
            put("sortOrder", sortOrder)
            favorite?.takeIf { it.isNotBlank() }?.let { put("favorite", it) }
            year?.takeIf { it.isNotBlank() }?.let { put("year", it) }
            month?.takeIf { it.isNotBlank() }?.let { put("month", it) }
            genres?.takeIf { it.isNotBlank() }?.let { put("genres", it) }
        }
    }
}

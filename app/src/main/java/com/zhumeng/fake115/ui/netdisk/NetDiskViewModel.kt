package com.zhumeng.fake115.ui.netdisk

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhumeng.fake115.data.NetDiskRepository
import com.zhumeng.fake115.data.model.FavoriteFilterMode
import com.zhumeng.fake115.data.model.NetDiskFile
import com.zhumeng.fake115.data.model.NetDiskPathNode
import com.zhumeng.fake115.data.model.NetDiskQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil

private const val LOAD_ERROR = "加载网盘文件失败。"

enum class NetDiskSortOption(
    val queryValue: String,
) {
    UpdateTime("user_utime"),
    FileName("file_name"),
    FileSize("file_size"),
    FileType("file_type"),
    ;

    fun next(): NetDiskSortOption {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

enum class NetDiskViewMode {
    List,
    Waterfall,
}

enum class NetDiskDurationSortOrder {
    Asc,
    Desc,
}

data class NetDiskUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadingAll: Boolean = false,
    val errorMessage: String? = null,
    val rawFiles: List<NetDiskFile> = emptyList(),
    val files: List<NetDiskFile> = emptyList(),
    val contentResetVersion: Int = 0,
    val count: Int = 0,
    val offset: Int = 0,
    val limit: Int = 50,
    val currentCid: String = "0",
    val path: List<NetDiskPathNode> = emptyList(),
    val onlyVideos: Boolean = false,
    val favoriteFilter: FavoriteFilterMode = FavoriteFilterMode.All,
    val sortOption: NetDiskSortOption = NetDiskSortOption.FileSize,
    val isAscending: Boolean = false,
    val durationSortOrder: NetDiskDurationSortOrder? = null,
    val viewMode: NetDiskViewMode = NetDiskViewMode.List,
    val starUpdatingIds: Set<String> = emptySet(),
    val deletingIds: Set<String> = emptySet(),
) {
    val hasMore: Boolean
        get() = rawFiles.size + offset < count

    val currentPage: Int
        get() = (offset / limit) + 1

    val totalPages: Int
        get() = maxOf(1, ceil(count / limit.toDouble()).toInt())

    val hasPreviousPage: Boolean
        get() = offset > 0

    val hasNextPage: Boolean
        get() = offset + limit < count
}

class NetDiskViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = NetDiskRepository(application)
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(loadSavedFilters())
    val uiState: StateFlow<NetDiskUiState> = _uiState.asStateFlow()

    init {
        observeRepositoryEvents()
        val current = _uiState.value
        loadFiles(
            cid = current.currentCid,
            offset = 0,
            isRefreshing = false,
            append = false,
            onlyVideos = current.onlyVideos,
        )
    }

    private fun observeRepositoryEvents() {
        viewModelScope.launch {
            NetDiskRepository.starredFileEvents.collect { event ->
                updateFileStarLocally(event.fileId, event.isStarred)
            }
        }
        viewModelScope.launch {
            NetDiskRepository.deletedFileEvents.collect { fileId ->
                removeFileLocally(fileId)
            }
        }
    }

    private fun updateFileStarLocally(fileId: String, isStarred: Boolean) {
        _uiState.update { state ->
            val rawFiles = state.rawFiles.map { file ->
                if (file.id == fileId) file.copy(isStarred = isStarred) else file
            }
            state.copy(
                rawFiles = rawFiles,
                files = rawFiles.toDisplayedFiles(state.favoriteFilter, state.durationSortOrder),
            )
        }
    }

    private fun removeFileLocally(fileId: String) {
        _uiState.update { state ->
            val existed = state.rawFiles.any { it.id == fileId }
            val rawFiles = state.rawFiles.filterNot { it.id == fileId }
            state.copy(
                rawFiles = rawFiles,
                files = rawFiles.toDisplayedFiles(state.favoriteFilter, state.durationSortOrder),
                count = if (existed) (state.count - 1).coerceAtLeast(0) else state.count,
            )
        }
    }

    fun refresh() {
        val current = _uiState.value
        loadFiles(
            cid = current.currentCid,
            offset = current.offset,
            isRefreshing = true,
            append = false,
            onlyVideos = current.onlyVideos,
        )
    }

    fun loadMore() {
        val current = _uiState.value
        if (current.isLoading || current.isRefreshing || current.isLoadingMore || !current.hasMore) return
        loadFiles(
            cid = current.currentCid,
            offset = current.offset + current.rawFiles.size,
            isRefreshing = false,
            append = true,
            onlyVideos = current.onlyVideos,
        )
    }

    fun openDirectory(file: NetDiskFile) {
        if (!file.isDirectory) return
        loadFiles(
            cid = file.id,
            offset = 0,
            isRefreshing = false,
            append = false,
            onlyVideos = _uiState.value.onlyVideos,
        )
    }

    fun openPath(cid: String) {
        loadFiles(
            cid = cid.ifBlank { "0" },
            offset = 0,
            isRefreshing = false,
            append = false,
            onlyVideos = _uiState.value.onlyVideos,
        )
    }

    fun navigateUp() {
        val path = _uiState.value.path
        val parentCid = if (path.size >= 2) path[path.lastIndex - 1].cid else "0"
        loadFiles(
            cid = parentCid.ifBlank { "0" },
            offset = 0,
            isRefreshing = false,
            append = false,
            onlyVideos = _uiState.value.onlyVideos,
        )
    }

    fun toggleVideoFilter() {
        val nextOnlyVideos = !_uiState.value.onlyVideos
        _uiState.update {
            it.copy(
                onlyVideos = nextOnlyVideos,
                durationSortOrder = if (nextOnlyVideos) it.durationSortOrder else null,
            )
        }
        saveFilters()
        reloadFromFirstPage(onlyVideos = nextOnlyVideos)
    }

    fun cycleFavoriteFilter() {
        _uiState.update { state ->
            val next = when (state.favoriteFilter) {
                FavoriteFilterMode.All -> FavoriteFilterMode.Favorite
                FavoriteFilterMode.Favorite -> FavoriteFilterMode.Unfavorite
                FavoriteFilterMode.Unfavorite -> FavoriteFilterMode.All
            }
            state.copy(
                favoriteFilter = next,
                files = state.rawFiles.toDisplayedFiles(next, state.durationSortOrder),
            )
        }
        saveFilters()
    }

    fun cycleSortOption() {
        _uiState.update { it.copy(sortOption = it.sortOption.next()) }
        saveFilters()
        reloadFromFirstPage(onlyVideos = _uiState.value.onlyVideos)
    }

    fun setSortOption(option: NetDiskSortOption) {
        _uiState.update { it.copy(sortOption = option) }
        saveFilters()
        reloadFromFirstPage(onlyVideos = _uiState.value.onlyVideos)
    }

    fun toggleSortOrder() {
        _uiState.update { it.copy(isAscending = !it.isAscending) }
        saveFilters()
        reloadFromFirstPage(onlyVideos = _uiState.value.onlyVideos)
    }

    fun cyclePageSize() {
        _uiState.update {
            it.copy(
                limit = when (it.limit) {
                    20 -> 50
                    50 -> 100
                    100 -> 1150
                    else -> 20
                }
            )
        }
        saveFilters()
        reloadFromFirstPage(onlyVideos = _uiState.value.onlyVideos)
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(
                viewMode = when (it.viewMode) {
                    NetDiskViewMode.List -> NetDiskViewMode.Waterfall
                    NetDiskViewMode.Waterfall -> NetDiskViewMode.List
                }
            )
        }
        saveFilters()
    }

    fun toggleDurationSort() {
        _uiState.update { state ->
            if (!state.onlyVideos) {
                state.copy(durationSortOrder = null)
            } else {
                val next = when (state.durationSortOrder) {
                    NetDiskDurationSortOrder.Asc -> NetDiskDurationSortOrder.Desc
                    NetDiskDurationSortOrder.Desc -> NetDiskDurationSortOrder.Asc
                    null -> NetDiskDurationSortOrder.Asc
                }
                state.copy(
                    durationSortOrder = next,
                    files = state.rawFiles.toDisplayedFiles(state.favoriteFilter, next),
                )
            }
        }
    }

    fun toggleFileStar(fileId: String) {
        val current = _uiState.value
        if (fileId in current.starUpdatingIds) return
        val file = current.rawFiles.firstOrNull { it.id == fileId } ?: return
        val nextStarred = !file.isStarred
        _uiState.update {
            it.copy(starUpdatingIds = it.starUpdatingIds + fileId)
        }
        viewModelScope.launch {
            runCatching {
                repository.updateStar(fileId = fileId, star = nextStarred)
            }.onFailure {
                // Keep the current list as-is; the repository event updates it after a successful request.
            }
            _uiState.update {
                it.copy(starUpdatingIds = it.starUpdatingIds - fileId)
            }
        }
    }

    fun deleteFile(fileId: String) {
        val current = _uiState.value
        if (fileId in current.deletingIds) return
        if (current.rawFiles.none { it.id == fileId }) return
        _uiState.update {
            it.copy(deletingIds = it.deletingIds + fileId)
        }
        viewModelScope.launch {
            runCatching {
                repository.deleteFile(fileId)
                NetDiskRepository.notifyFileDeleted(fileId)
            }
            _uiState.update {
                it.copy(deletingIds = it.deletingIds - fileId)
            }
        }
    }

    private fun loadSavedFilters(): NetDiskUiState {
        return NetDiskUiState(
            onlyVideos = prefs.getBoolean(KEY_ONLY_VIDEOS, false),
            favoriteFilter = FavoriteFilterMode.entries.firstOrNull {
                it.name == prefs.getString(KEY_FAVORITE_FILTER, FavoriteFilterMode.All.name)
            } ?: FavoriteFilterMode.All,
            sortOption = NetDiskSortOption.entries.firstOrNull {
                it.queryValue == prefs.getString(KEY_SORT_OPTION, NetDiskSortOption.FileSize.queryValue)
            } ?: NetDiskSortOption.FileSize,
            isAscending = prefs.getBoolean(KEY_IS_ASCENDING, false),
            limit = prefs.getInt(KEY_PAGE_SIZE, DEFAULT_PAGE_SIZE).takeIf {
                it in PAGE_SIZE_OPTIONS
            } ?: DEFAULT_PAGE_SIZE,
            currentCid = prefs.getString(KEY_CURRENT_CID, "0")?.ifBlank { "0" } ?: "0",
            viewMode = NetDiskViewMode.entries.firstOrNull {
                it.name == prefs.getString(KEY_VIEW_MODE, NetDiskViewMode.List.name)
            } ?: NetDiskViewMode.List,
        )
    }

    private fun saveFilters() {
        val current = _uiState.value
        prefs.edit()
            .putBoolean(KEY_ONLY_VIDEOS, current.onlyVideos)
            .putString(KEY_FAVORITE_FILTER, current.favoriteFilter.name)
            .putString(KEY_SORT_OPTION, current.sortOption.queryValue)
            .putBoolean(KEY_IS_ASCENDING, current.isAscending)
            .putInt(KEY_PAGE_SIZE, current.limit)
            .putString(KEY_VIEW_MODE, current.viewMode.name)
            .apply()
    }

    fun previousPage(): Int? {
        val current = _uiState.value
        if (
            current.isLoading ||
            current.isRefreshing ||
            current.isLoadingMore ||
            current.isLoadingAll ||
            !current.hasPreviousPage
        ) {
            return null
        }
        val targetOffset = (current.offset - current.limit).coerceAtLeast(0)
        loadFiles(
            cid = current.currentCid,
            offset = targetOffset,
            isRefreshing = false,
            append = false,
            onlyVideos = current.onlyVideos,
        )
        return (targetOffset / current.limit) + 1
    }

    fun nextPage(): Int? {
        val current = _uiState.value
        if (
            current.isLoading ||
            current.isRefreshing ||
            current.isLoadingMore ||
            current.isLoadingAll ||
            !current.hasNextPage
        ) {
            return null
        }
        val targetOffset = current.offset + current.limit
        loadFiles(
            cid = current.currentCid,
            offset = targetOffset,
            isRefreshing = false,
            append = false,
            onlyVideos = current.onlyVideos,
        )
        return (targetOffset / current.limit) + 1
    }

    fun goToPage(page: Int): Int? {
        val current = _uiState.value
        if (
            current.isLoading ||
            current.isRefreshing ||
            current.isLoadingMore ||
            current.isLoadingAll ||
            page !in 1..current.totalPages
        ) {
            return null
        }
        val targetOffset = ((page - 1) * current.limit).coerceAtLeast(0)
        loadFiles(
            cid = current.currentCid,
            offset = targetOffset,
            isRefreshing = false,
            append = false,
            onlyVideos = current.onlyVideos,
        )
        return page
    }

    fun loadAllFiles() {
        val current = _uiState.value
        if (current.isLoading || current.isRefreshing || current.isLoadingMore || current.isLoadingAll) return
        viewModelScope.launch {
            val start = _uiState.value
            val pageLimit = LOAD_ALL_PAGE_SIZE
            _uiState.update {
                it.copy(
                    isLoadingAll = true,
                    errorMessage = null,
                    limit = pageLimit,
                    offset = 0,
                    contentResetVersion = it.contentResetVersion + 1,
                )
            }

            runCatching {
                val allFiles = mutableListOf<NetDiskFile>()
                var nextOffset = 0
                var totalCount = 0
                var responseCid = start.currentCid
                var responsePath = start.path

                do {
                    val response = repository.fetchFiles(
                        NetDiskQuery(
                            cid = start.currentCid,
                            type = if (start.onlyVideos) "4" else "",
                            limit = pageLimit,
                            offset = nextOffset,
                            asc = if (start.isAscending) 1 else 0,
                            orderBy = start.sortOption.queryValue,
                            showDir = 1,
                        )
                    )
                    allFiles += response.files
                    totalCount = response.count
                    responseCid = response.cid.ifBlank { start.currentCid }
                    responsePath = response.path
                    nextOffset = response.offset + response.files.size
                } while (allFiles.size < totalCount && nextOffset < totalCount && response.files.isNotEmpty())

                LoadedNetDiskFiles(
                    files = allFiles,
                    count = totalCount,
                    cid = responseCid,
                    path = responsePath,
                )
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isLoadingAll = false,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        rawFiles = result.files,
                        files = result.files.toDisplayedFiles(it.favoriteFilter, it.durationSortOrder),
                        count = result.count,
                        offset = 0,
                        limit = pageLimit,
                        currentCid = result.cid,
                        path = result.path,
                    )
                }
                prefs.edit()
                    .putInt(KEY_PAGE_SIZE, pageLimit)
                    .putString(KEY_CURRENT_CID, result.cid)
                    .apply()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingAll = false,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = error.message ?: LOAD_ERROR,
                    )
                }
            }
        }
    }

    private fun reloadFromFirstPage(onlyVideos: Boolean) {
        loadFiles(
            cid = _uiState.value.currentCid,
            offset = 0,
            isRefreshing = false,
            append = false,
            onlyVideos = onlyVideos,
        )
    }

    private fun loadFiles(
        cid: String,
        offset: Int,
        isRefreshing: Boolean,
        append: Boolean,
        onlyVideos: Boolean,
    ) {
        viewModelScope.launch {
            val current = _uiState.value
            _uiState.update {
                it.copy(
                    isLoading = !isRefreshing && !append,
                    isRefreshing = isRefreshing,
                    isLoadingMore = append,
                    errorMessage = null,
                    currentCid = if (append) it.currentCid else cid,
                    offset = if (append) it.offset else offset,
                    contentResetVersion = if (append) it.contentResetVersion else it.contentResetVersion + 1,
                )
            }

            runCatching {
                repository.fetchFiles(
                    NetDiskQuery(
                        cid = cid,
                        type = if (onlyVideos) "4" else "",
                        limit = current.limit,
                        offset = offset,
                        asc = if (current.isAscending) 1 else 0,
                        orderBy = current.sortOption.queryValue,
                        showDir = 1,
                    )
                )
            }.onSuccess { response ->
                _uiState.update {
                    val rawFiles = if (append) it.rawFiles + response.files else response.files
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = null,
                        rawFiles = rawFiles,
                        files = rawFiles.toDisplayedFiles(it.favoriteFilter, it.durationSortOrder),
                        count = response.count,
                        offset = if (append) it.offset else response.offset,
                        limit = response.limit,
                        currentCid = response.cid.ifBlank { cid },
                        path = response.path,
                    )
                }
                if (!append) {
                    prefs.edit()
                        .putString(KEY_CURRENT_CID, response.cid.ifBlank { cid })
                        .apply()
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = error.message ?: LOAD_ERROR,
                    )
                }
            }
        }
    }

    private companion object {
        const val PREFS_NAME = "netdisk_prefs"
        const val KEY_ONLY_VIDEOS = "only_videos"
        const val KEY_FAVORITE_FILTER = "favorite_filter"
        const val KEY_SORT_OPTION = "sort_option"
        const val KEY_IS_ASCENDING = "is_ascending"
        const val KEY_PAGE_SIZE = "page_size"
        const val KEY_CURRENT_CID = "current_cid"
        const val KEY_VIEW_MODE = "view_mode"
        const val DEFAULT_PAGE_SIZE = 50
        const val LOAD_ALL_PAGE_SIZE = 1150
        val PAGE_SIZE_OPTIONS = setOf(20, 50, 100, 1150)
    }
}

private data class LoadedNetDiskFiles(
    val files: List<NetDiskFile>,
    val count: Int,
    val cid: String,
    val path: List<NetDiskPathNode>,
)

private fun List<NetDiskFile>.toDisplayedFiles(
    filter: FavoriteFilterMode,
    durationSortOrder: NetDiskDurationSortOrder?,
): List<NetDiskFile> {
    val filtered = when (filter) {
        FavoriteFilterMode.All -> this
        FavoriteFilterMode.Favorite -> filter { it.isStarred }
        FavoriteFilterMode.Unfavorite -> filterNot { it.isStarred }
    }
    return when (durationSortOrder) {
        NetDiskDurationSortOrder.Asc -> filtered.sortedWith(
            compareBy<NetDiskFile> { it.durationSeconds == null }
                .thenBy { it.durationSeconds ?: Long.MAX_VALUE }
        )
        NetDiskDurationSortOrder.Desc -> filtered.sortedWith(
            compareBy<NetDiskFile> { it.durationSeconds == null }
                .thenByDescending { it.durationSeconds ?: Long.MIN_VALUE }
        )
        null -> filtered
    }
}

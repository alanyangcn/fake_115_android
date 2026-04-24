package com.zhumeng.fake115.ui.netdisk

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhumeng.fake115.data.NetDiskRepository
import com.zhumeng.fake115.data.model.NetDiskFile
import com.zhumeng.fake115.data.model.NetDiskPathNode
import com.zhumeng.fake115.data.model.NetDiskQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil

private const val LOAD_ERROR = "\u52a0\u8f7d\u7f51\u76d8\u6587\u4ef6\u5931\u8d25\u3002"

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

data class NetDiskUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val files: List<NetDiskFile> = emptyList(),
    val count: Int = 0,
    val offset: Int = 0,
    val limit: Int = 50,
    val currentCid: String = "0",
    val path: List<NetDiskPathNode> = emptyList(),
    val onlyVideos: Boolean = false,
    val sortOption: NetDiskSortOption = NetDiskSortOption.FileSize,
    val isAscending: Boolean = false,
) {
    val hasMore: Boolean
        get() = files.size + offset < count

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
    private val _uiState = MutableStateFlow(NetDiskUiState())
    val uiState: StateFlow<NetDiskUiState> = _uiState.asStateFlow()

    init {
        loadFiles(
            cid = "0",
            offset = 0,
            isRefreshing = false,
            append = false,
            onlyVideos = false,
        )
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
            offset = current.offset + current.files.size,
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
        _uiState.update { it.copy(onlyVideos = nextOnlyVideos) }
        reloadFromFirstPage(onlyVideos = nextOnlyVideos)
    }

    fun cycleSortOption() {
        _uiState.update { it.copy(sortOption = it.sortOption.next()) }
        reloadFromFirstPage(onlyVideos = _uiState.value.onlyVideos)
    }

    fun toggleSortOrder() {
        _uiState.update { it.copy(isAscending = !it.isAscending) }
        reloadFromFirstPage(onlyVideos = _uiState.value.onlyVideos)
    }

    fun cyclePageSize() {
        _uiState.update {
            it.copy(
                limit = when (it.limit) {
                    20 -> 50
                    50 -> 100
                    else -> 20
                }
            )
        }
        reloadFromFirstPage(onlyVideos = _uiState.value.onlyVideos)
    }

    fun previousPage(): Int? {
        val current = _uiState.value
        if (current.isLoading || current.isRefreshing || current.isLoadingMore || !current.hasPreviousPage) return null
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
        if (current.isLoading || current.isRefreshing || current.isLoadingMore || !current.hasNextPage) return null
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
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = null,
                        files = if (append) it.files + response.files else response.files,
                        count = response.count,
                        offset = if (append) it.offset else response.offset,
                        limit = response.limit,
                        currentCid = response.cid.ifBlank { cid },
                        path = response.path,
                    )
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
}

package com.zhumeng.fake115.ui.actress

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhumeng.fake115.data.ActressRepository
import com.zhumeng.fake115.data.model.Actress
import com.zhumeng.fake115.data.model.ActressQuery
import com.zhumeng.fake115.data.model.FavoriteFilterMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActressUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val favoriteUpdatingIds: Set<Int> = emptySet(),
    val videosFavoriteUpdatingIds: Set<Int> = emptySet(),
    val errorMessage: String? = null,
    val actresses: List<Actress> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 115,
    val searchInput: String = "",
    val sortOrder: String = "desc",
    val favoriteFilter: FavoriteFilterMode = FavoriteFilterMode.All,
) {
    val hasMore: Boolean
        get() = actresses.size < total
}

private data class SavedActressState(
    val searchInput: String,
    val sortOrder: String,
    val favoriteFilter: FavoriteFilterMode,
)

class ActressViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = ActressRepository()
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(
        ActressUiState().applySavedState(readSavedState())
    )
    val uiState: StateFlow<ActressUiState> = _uiState.asStateFlow()
    private val _toastMessages = MutableSharedFlow<String>()
    val toastMessages: SharedFlow<String> = _toastMessages.asSharedFlow()

    private var searchJob: Job? = null

    init {
        loadActresses(page = 1, isRefreshing = false, append = false)
    }

    fun onSearchInputChanged(value: String) {
        _uiState.update { it.copy(searchInput = value) }
        persistCurrentControls()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            loadActresses(page = 1, isRefreshing = false, append = false)
        }
    }

    fun refresh() {
        loadActresses(page = 1, isRefreshing = true, append = false)
    }

    fun toggleSortOrder() {
        _uiState.update {
            it.copy(sortOrder = if (it.sortOrder == "desc") "asc" else "desc")
        }
        persistCurrentControls()
        loadActresses(page = 1, isRefreshing = false, append = false)
    }

    fun cycleFavoriteFilter() {
        _uiState.update {
            val next = when (it.favoriteFilter) {
                FavoriteFilterMode.All -> FavoriteFilterMode.Favorite
                FavoriteFilterMode.Favorite -> FavoriteFilterMode.Unfavorite
                FavoriteFilterMode.Unfavorite -> FavoriteFilterMode.All
            }
            it.copy(favoriteFilter = next)
        }
        persistCurrentControls()
        loadActresses(page = 1, isRefreshing = false, append = false)
    }

    fun loadMore() {
        val current = _uiState.value
        if (current.isLoading || current.isRefreshing || current.isLoadingMore || !current.hasMore) return
        loadActresses(page = current.page + 1, isRefreshing = false, append = true)
    }

    fun toggleFavorite(actressId: Int) {
        val current = _uiState.value
        if (actressId in current.favoriteUpdatingIds) return

        val actress = current.actresses.firstOrNull { it.id == actressId } ?: return
        val targetFavorite = actress.isFavorite != 1

        _uiState.update {
            it.copy(favoriteUpdatingIds = it.favoriteUpdatingIds + actressId, errorMessage = null)
        }

        viewModelScope.launch {
            runCatching {
                repository.updateFavorite(actressId = actressId, favorite = targetFavorite)
            }.onSuccess { favorite ->
                _uiState.update { state ->
                    val shouldRemove =
                        (state.favoriteFilter == FavoriteFilterMode.Favorite && !favorite) ||
                            (state.favoriteFilter == FavoriteFilterMode.Unfavorite && favorite)

                    state.copy(
                        actresses = state.actresses
                            .map { row ->
                                if (row.id == actressId) row.copy(isFavorite = if (favorite) 1 else 0) else row
                            }
                            .let { rows -> if (shouldRemove) rows.filterNot { it.id == actressId } else rows },
                        total = if (shouldRemove) (state.total - 1).coerceAtLeast(0) else state.total,
                        favoriteUpdatingIds = state.favoriteUpdatingIds - actressId,
                    )
                }
                _toastMessages.emit(
                    if (favorite) "已收藏演员" else "已取消收藏演员"
                )
            }.onFailure { error ->
                val message = error.message ?: "收藏演员失败"
                _uiState.update {
                    it.copy(
                        favoriteUpdatingIds = it.favoriteUpdatingIds - actressId,
                        errorMessage = message,
                    )
                }
                _toastMessages.emit(message)
            }
        }
    }

    fun toggleAllVideosFavorite(actressId: Int) {
        val current = _uiState.value
        if (actressId in current.videosFavoriteUpdatingIds) return

        val actress = current.actresses.firstOrNull { it.id == actressId } ?: return
        val targetFavorite = actress.isFavoriteAllVideos != 1

        _uiState.update {
            it.copy(videosFavoriteUpdatingIds = it.videosFavoriteUpdatingIds + actressId, errorMessage = null)
        }

        viewModelScope.launch {
            runCatching {
                repository.updateVideosFavorite(actressId = actressId, favorite = targetFavorite)
            }.onSuccess { result ->
                _uiState.update { state ->
                    state.copy(
                        actresses = state.actresses.map { row ->
                            if (row.id == actressId) {
                                row.copy(isFavoriteAllVideos = if (result.favorite) 1 else 0)
                            } else {
                                row
                            }
                        },
                        videosFavoriteUpdatingIds = state.videosFavoriteUpdatingIds - actressId,
                    )
                }
                val action = if (result.favorite) "收藏" else "取消收藏"
                val deletedMissingText = if (result.deletedMissingCount > 0) {
                    "，清理失效 ${result.deletedMissingCount} 部"
                } else {
                    ""
                }
                _toastMessages.emit("已${action} ${result.count} 部影片$deletedMissingText")
            }.onFailure { error ->
                val message = error.message ?: "批量收藏影片失败"
                _uiState.update {
                    it.copy(
                        videosFavoriteUpdatingIds = it.videosFavoriteUpdatingIds - actressId,
                        errorMessage = message,
                    )
                }
                _toastMessages.emit(message)
            }
        }
    }

    private fun loadActresses(page: Int, isRefreshing: Boolean, append: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value
            _uiState.update {
                it.copy(
                    isLoading = !isRefreshing && !append,
                    isRefreshing = isRefreshing,
                    isLoadingMore = append,
                    errorMessage = null,
                )
            }

            runCatching {
                repository.fetchActresses(
                    ActressQuery(
                        page = page,
                        limit = current.limit,
                        search = current.searchInput.trim(),
                        sortOrder = current.sortOrder,
                        favorite = current.favoriteFilter.toQueryValue(),
                    )
                )
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = null,
                        actresses = if (append) it.actresses + response.actresses else response.actresses,
                        total = response.total,
                        page = response.page,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = error.message ?: "加载演员列表失败。",
                    )
                }
            }
        }
    }

    private fun readSavedState(): SavedActressState {
        return SavedActressState(
            searchInput = prefs.getString(KEY_SEARCH_INPUT, "").orEmpty(),
            sortOrder = prefs.getString(KEY_SORT_ORDER, "desc") ?: "desc",
            favoriteFilter = FavoriteFilterMode.entries.firstOrNull {
                it.name == prefs.getString(KEY_FAVORITE_FILTER, FavoriteFilterMode.All.name)
            } ?: FavoriteFilterMode.All,
        )
    }

    private fun persistCurrentControls() {
        val current = _uiState.value
        prefs.edit()
            .putString(KEY_SEARCH_INPUT, current.searchInput)
            .putString(KEY_SORT_ORDER, current.sortOrder)
            .putString(KEY_FAVORITE_FILTER, current.favoriteFilter.name)
            .apply()
    }

    private fun ActressUiState.applySavedState(saved: SavedActressState): ActressUiState {
        return copy(
            searchInput = saved.searchInput,
            sortOrder = saved.sortOrder,
            favoriteFilter = saved.favoriteFilter,
        )
    }

    companion object {
        private const val PREFS_NAME = "actress_prefs"
        private const val KEY_SEARCH_INPUT = "search_input"
        private const val KEY_SORT_ORDER = "sort_order"
        private const val KEY_FAVORITE_FILTER = "favorite_filter"
    }
}

private fun FavoriteFilterMode.toQueryValue(): String? {
    return when (this) {
        FavoriteFilterMode.All -> null
        FavoriteFilterMode.Favorite -> "1"
        FavoriteFilterMode.Unfavorite -> "0"
    }
}

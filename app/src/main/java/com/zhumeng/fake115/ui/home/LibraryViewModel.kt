package com.zhumeng.fake115.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhumeng.fake115.data.LibraryRepository
import com.zhumeng.fake115.data.model.FavoriteFilterMode
import com.zhumeng.fake115.data.model.GenreOption
import com.zhumeng.fake115.data.model.LibraryMovie
import com.zhumeng.fake115.data.model.LibraryQuery
import com.zhumeng.fake115.data.model.SortOption
import com.zhumeng.fake115.data.model.ViewMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val favoriteUpdatingIds: Set<Int> = emptySet(),
    val deletingIds: Set<Int> = emptySet(),
    val errorMessage: String? = null,
    val movies: List<LibraryMovie> = emptyList(),
    val genres: List<GenreOption> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 30,
    val searchInput: String = "",
    val sortOption: SortOption = SortOption.ReleaseDate,
    val sortOrder: String = "desc",
    val favoriteFilter: FavoriteFilterMode = FavoriteFilterMode.All,
    val viewMode: ViewMode = ViewMode.Normal,
    val selectedYear: String? = null,
    val selectedMonth: String? = null,
    val selectedGenres: Set<String> = emptySet(),
    val selectedActress: String? = null,
    val selectedStudio: String? = null,
    val selectedPublisher: String? = null,
    val selectedSeries: String? = null,
    val selectedFanhaoSeries: String? = null,
) {
    val hasMore: Boolean
        get() = movies.size < total

    val activeDetailFilterLabel: String?
        get() = when {
            selectedStudio != null -> "制作商：$selectedStudio"
            selectedPublisher != null -> "发行商：$selectedPublisher"
            selectedSeries != null -> "系列：$selectedSeries"
            selectedFanhaoSeries != null -> "番号系列：$selectedFanhaoSeries"
            selectedActress != null -> "演出人员：$selectedActress"
            selectedGenres.size == 1 -> "类别：${selectedGenres.first()}"
            selectedGenres.size > 1 -> "类别：${selectedGenres.size} 项"
            else -> null
        }
}

private data class SavedLibraryState(
    val sortOption: SortOption,
    val sortOrder: String,
    val favoriteFilter: FavoriteFilterMode,
    val viewMode: ViewMode,
    val limit: Int,
    val selectedActress: String?,
)

class LibraryViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = LibraryRepository(application)
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(
        LibraryUiState().applySavedState(readSavedState())
    )
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    private val _toastMessages = MutableSharedFlow<String>()
    val toastMessages: SharedFlow<String> = _toastMessages.asSharedFlow()

    private var searchJob: Job? = null

    init {
        observeRepositoryEvents()
        loadInitial()
    }

    fun onSearchInputChanged(value: String) {
        _uiState.update { it.copy(searchInput = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(450)
            loadLibrary(page = 1, isRefreshing = false, append = false)
        }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        persistCurrentControls()
        loadLibrary(page = 1, isRefreshing = false, append = false)
    }

    fun cyclePageSize() {
        _uiState.update {
            it.copy(limit = nextPageSize(it.limit))
        }
        persistCurrentControls()
        loadLibrary(page = 1, isRefreshing = false, append = false)
    }

    fun toggleSortOrder() {
        _uiState.update {
            it.copy(sortOrder = if (it.sortOrder == "desc") "asc" else "desc")
        }
        persistCurrentControls()
        loadLibrary(page = 1, isRefreshing = false, append = false)
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
        loadLibrary(page = 1, isRefreshing = false, append = false)
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(
                viewMode = if (it.viewMode == ViewMode.Normal) ViewMode.Compact else ViewMode.Normal
            )
        }
        persistCurrentControls()
    }

    fun toggleYear(year: String) {
        _uiState.update {
            it.copy(selectedYear = if (it.selectedYear == year) null else year)
        }
        loadLibrary(page = 1, isRefreshing = false, append = false)
    }

    fun toggleMonth(month: String) {
        _uiState.update {
            it.copy(selectedMonth = if (it.selectedMonth == month) null else month)
        }
        loadLibrary(page = 1, isRefreshing = false, append = false)
    }

    fun toggleGenre(name: String) {
        _uiState.update { current ->
            val next = current.selectedGenres.toMutableSet()
            if (!next.add(name)) next.remove(name)
            current.copy(selectedGenres = next)
        }
        loadLibrary(page = 1, isRefreshing = false, append = false)
    }

    fun filterByActress(name: String) {
        filterByDetailTag(LibraryDetailFilterType.Actress, name)
    }

    fun filterByDetailTag(type: LibraryDetailFilterType, value: String) {
        val tag = value.trim()
        if (tag.isBlank()) return

        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchInput = if (type == LibraryDetailFilterType.FanhaoSeries) tag else "",
                selectedYear = null,
                selectedMonth = null,
                selectedGenres = if (type == LibraryDetailFilterType.Genre) setOf(tag) else emptySet(),
                selectedActress = if (type == LibraryDetailFilterType.Actress) tag else null,
                selectedStudio = if (type == LibraryDetailFilterType.Studio) tag else null,
                selectedPublisher = if (type == LibraryDetailFilterType.Publisher) tag else null,
                selectedSeries = if (type == LibraryDetailFilterType.Series) tag else null,
                selectedFanhaoSeries = if (type == LibraryDetailFilterType.FanhaoSeries) tag else null,
            )
        }
        persistCurrentControls()
        loadLibrary(page = 1, isRefreshing = false, append = false)
    }

    fun clearDetailFilter() {
        _uiState.update {
            it.copy(
                selectedActress = null,
                selectedGenres = emptySet(),
                selectedStudio = null,
                selectedPublisher = null,
                selectedSeries = null,
                selectedFanhaoSeries = null,
                searchInput = if (it.selectedFanhaoSeries != null) "" else it.searchInput,
            )
        }
        persistCurrentControls()
        loadLibrary(page = 1, isRefreshing = false, append = false)
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedYear = null,
                selectedMonth = null,
                selectedGenres = emptySet(),
                selectedActress = null,
                selectedStudio = null,
                selectedPublisher = null,
                selectedSeries = null,
                selectedFanhaoSeries = null,
                favoriteFilter = FavoriteFilterMode.All,
            )
        }
        persistCurrentControls()
        loadLibrary(page = 1, isRefreshing = false, append = false)
    }

    fun refresh() {
        loadLibrary(page = 1, isRefreshing = true, append = false)
    }

    fun loadMore() {
        val current = _uiState.value
        if (current.isLoading || current.isRefreshing || current.isLoadingMore || !current.hasMore) return
        loadLibrary(page = current.page + 1, isRefreshing = false, append = true)
    }

    fun toggleFavorite(movieId: Int) {
        val current = _uiState.value
        if (movieId in current.favoriteUpdatingIds) return

        val movie = current.movies.firstOrNull { it.id == movieId } ?: return
        val targetFavorite = movie.isFavorite != 1

        _uiState.update {
            it.copy(favoriteUpdatingIds = it.favoriteUpdatingIds + movieId, errorMessage = null)
        }

        viewModelScope.launch {
            runCatching {
                repository.updateFavorite(movieId = movieId, favorite = targetFavorite)
            }.onSuccess { favorite ->
                _uiState.update { state ->
                    val shouldRemove =
                        (state.favoriteFilter == FavoriteFilterMode.Favorite && !favorite) ||
                            (state.favoriteFilter == FavoriteFilterMode.Unfavorite && favorite)

                    state.copy(
                        movies = state.movies
                            .map { row ->
                                if (row.id == movieId) row.copy(isFavorite = if (favorite) 1 else 0) else row
                            }
                            .let { rows -> if (shouldRemove) rows.filterNot { it.id == movieId } else rows },
                        total = if (shouldRemove) (state.total - 1).coerceAtLeast(0) else state.total,
                        favoriteUpdatingIds = state.favoriteUpdatingIds - movieId,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        favoriteUpdatingIds = it.favoriteUpdatingIds - movieId,
                        errorMessage = error.message ?: "收藏操作失败。",
                    )
                }
            }
        }
    }

    fun deleteMovie(movieId: Int) {
        val current = _uiState.value
        if (movieId in current.deletingIds) return

        _uiState.update {
            it.copy(deletingIds = it.deletingIds + movieId, errorMessage = null)
        }

        viewModelScope.launch {
            runCatching {
                repository.deleteMovie(movieId)
            }.onSuccess { message ->
                removeMovieLocally(movieId, deletingCompleted = true)
                _toastMessages.emit(message)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        deletingIds = it.deletingIds - movieId,
                        errorMessage = error.message ?: "删除失败。",
                    )
                }
                _toastMessages.emit(error.message ?: "删除失败。")
            }
        }
    }

    private fun observeRepositoryEvents() {
        viewModelScope.launch {
            LibraryRepository.deletedMovieEvents.collect { movieId ->
                removeMovieLocally(movieId, deletingCompleted = true)
            }
        }
    }

    private fun removeMovieLocally(movieId: Int, deletingCompleted: Boolean) {
        _uiState.update { state ->
            val existed = state.movies.any { it.id == movieId }
            state.copy(
                movies = if (existed) state.movies.filterNot { it.id == movieId } else state.movies,
                total = if (existed) (state.total - 1).coerceAtLeast(0) else state.total,
                deletingIds = if (deletingCompleted) state.deletingIds - movieId else state.deletingIds,
            )
        }
    }

    private fun loadInitial() {
        viewModelScope.launch {
            runCatching { repository.fetchGenres() }
                .onSuccess { rows ->
                    _uiState.update { it.copy(genres = rows) }
                }

            loadLibrary(page = 1, isRefreshing = false, append = false)
        }
    }

    private fun loadLibrary(page: Int, isRefreshing: Boolean, append: Boolean) {
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
                repository.fetchLibrary(
                    LibraryQuery(
                        page = page,
                        limit = current.limit,
                        search = current.searchInput.trim(),
                        sortBy = current.sortOption.queryValue,
                        sortOrder = current.sortOrder,
                        favorite = current.favoriteFilter.toQueryValue(),
                        year = current.selectedYear,
                        month = current.selectedMonth,
                        genres = current.selectedGenres.takeIf { it.isNotEmpty() }?.joinToString(","),
                        actress = current.selectedActress,
                        studio = current.selectedStudio,
                        publisher = current.selectedPublisher,
                        series = current.selectedSeries,
                        fanhaoSeries = current.selectedFanhaoSeries,
                    )
                )
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = null,
                        movies = if (append) it.movies + response.videos else response.videos,
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
                        errorMessage = error.message ?: "Failed to load library.",
                    )
                }
            }
        }
    }

    private fun readSavedState(): SavedLibraryState {
        return SavedLibraryState(
            sortOption = SortOption.entries.firstOrNull {
                it.queryValue == prefs.getString(KEY_SORT_OPTION, SortOption.ReleaseDate.queryValue)
            } ?: SortOption.ReleaseDate,
            sortOrder = prefs.getString(KEY_SORT_ORDER, "desc") ?: "desc",
            favoriteFilter = FavoriteFilterMode.entries.firstOrNull {
                it.name == prefs.getString(KEY_FAVORITE_FILTER, FavoriteFilterMode.All.name)
            } ?: FavoriteFilterMode.All,
            viewMode = ViewMode.entries.firstOrNull {
                it.name == prefs.getString(KEY_VIEW_MODE, ViewMode.Normal.name)
            } ?: ViewMode.Normal,
            limit = prefs.getInt(KEY_PAGE_SIZE, DEFAULT_PAGE_SIZE).takeIf {
                it in PAGE_SIZE_OPTIONS
            } ?: DEFAULT_PAGE_SIZE,
            selectedActress = prefs.getString(KEY_SELECTED_ACTRESS, null)?.takeIf { it.isNotBlank() },
        )
    }

    private fun persistCurrentControls() {
        val current = _uiState.value
        prefs.edit()
            .putString(KEY_SORT_OPTION, current.sortOption.queryValue)
            .putString(KEY_SORT_ORDER, current.sortOrder)
            .putString(KEY_FAVORITE_FILTER, current.favoriteFilter.name)
            .putString(KEY_VIEW_MODE, current.viewMode.name)
            .putInt(KEY_PAGE_SIZE, current.limit)
            .putString(KEY_SELECTED_ACTRESS, current.selectedActress.orEmpty())
            .apply()
    }

    private fun LibraryUiState.applySavedState(saved: SavedLibraryState): LibraryUiState {
        return copy(
            sortOption = saved.sortOption,
            sortOrder = saved.sortOrder,
            favoriteFilter = saved.favoriteFilter,
            viewMode = saved.viewMode,
            limit = saved.limit,
            selectedActress = saved.selectedActress,
        )
    }

    companion object {
        private const val PREFS_NAME = "library_prefs"
        private const val KEY_SORT_OPTION = "sort_option"
        private const val KEY_SORT_ORDER = "sort_order"
        private const val KEY_FAVORITE_FILTER = "favorite_filter"
        private const val KEY_VIEW_MODE = "view_mode"
        private const val KEY_PAGE_SIZE = "page_size"
        private const val KEY_SELECTED_ACTRESS = "selected_actress"
        private const val DEFAULT_PAGE_SIZE = 30
        private val PAGE_SIZE_OPTIONS = listOf(30, 60, 120)
    }
}

enum class LibraryDetailFilterType(val queryKey: String) {
    Studio("studio"),
    Publisher("publisher"),
    Series("series"),
    Genre("genres"),
    Actress("actress"),
    FanhaoSeries("fanhaoSeries");

    companion object {
        fun fromQueryKey(value: String): LibraryDetailFilterType? {
            return entries.firstOrNull { it.queryKey == value }
        }
    }
}

private fun nextPageSize(current: Int): Int {
    val options = listOf(30, 60, 120)
    val index = options.indexOf(current)
    return if (index == -1 || index == options.lastIndex) options.first() else options[index + 1]
}

private fun FavoriteFilterMode.toQueryValue(): String? {
    return when (this) {
        FavoriteFilterMode.All -> null
        FavoriteFilterMode.Favorite -> "1"
        FavoriteFilterMode.Unfavorite -> "0"
    }
}

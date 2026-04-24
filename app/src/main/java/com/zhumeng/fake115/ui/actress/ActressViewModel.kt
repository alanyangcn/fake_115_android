package com.zhumeng.fake115.ui.actress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhumeng.fake115.data.ActressRepository
import com.zhumeng.fake115.data.model.Actress
import com.zhumeng.fake115.data.model.ActressQuery
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActressUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val actresses: List<Actress> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 115,
    val searchInput: String = "",
) {
    val hasMore: Boolean
        get() = actresses.size < total
}

class ActressViewModel : ViewModel() {
    private val repository = ActressRepository()
    private val _uiState = MutableStateFlow(ActressUiState())
    val uiState: StateFlow<ActressUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadActresses(page = 1, isRefreshing = false, append = false)
    }

    fun onSearchInputChanged(value: String) {
        _uiState.update { it.copy(searchInput = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            loadActresses(page = 1, isRefreshing = false, append = false)
        }
    }

    fun refresh() {
        loadActresses(page = 1, isRefreshing = true, append = false)
    }

    fun loadMore() {
        val current = _uiState.value
        if (current.isLoading || current.isRefreshing || current.isLoadingMore || !current.hasMore) return
        loadActresses(page = current.page + 1, isRefreshing = false, append = true)
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
}

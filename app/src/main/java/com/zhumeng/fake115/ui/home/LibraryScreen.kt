package com.zhumeng.fake115.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.zhumeng.fake115.data.AppSettings
import com.zhumeng.fake115.data.model.FavoriteFilterMode
import com.zhumeng.fake115.data.model.LibraryMovie
import com.zhumeng.fake115.data.model.SortOption
import com.zhumeng.fake115.data.model.ViewMode
import com.zhumeng.fake115.ui.common.DeleteConfirmDialog
import com.zhumeng.fake115.ui.theme.AppTheme
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun LibraryScreen(
    onOpenPlayer: (LibraryMovie, List<LibraryMovie>) -> Unit,
    onOpenDetail: (LibraryMovie) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: LibraryViewModel = viewModel(),
) {
    val colors = AppTheme.colors
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val quickManagementEnabled by AppSettings.quickManagementEnabledFlow(context).collectAsState()
    val gridState = rememberLazyGridState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
    )
    val currentYear = remember { LocalDate.now().year }
    val yearOptions = remember(currentYear) { (currentYear downTo 2007).map(Int::toString) }
    val monthOptions = remember { (1..12).map { it.toString().padStart(2, '0') } }
    var filterDialogVisible by remember { mutableStateOf(false) }
    var pendingDeleteMovie by remember { mutableStateOf<LibraryMovie?>(null) }
    val activeDetailFilterLabel = state.activeDetailFilterLabel
    val listTopPadding = contentPadding.calculateTopPadding() + if (activeDetailFilterLabel != null) 104.dp else 58.dp

    LaunchedEffect(viewModel, context) {
        viewModel.toastMessages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(state.deletingIds, state.movies, pendingDeleteMovie) {
        val movie = pendingDeleteMovie ?: return@LaunchedEffect
        val stillExists = state.movies.any { it.id == movie.id }
        val stillDeleting = movie.id in state.deletingIds
        if (!stillExists && !stillDeleting) {
            pendingDeleteMovie = null
        }
    }

    LaunchedEffect(gridState, state.movies.size, state.hasMore, state.isLoadingMore) {
        gridState.loadMoreWhenNeeded(
            itemCount = state.movies.size,
            hasMore = state.hasMore,
            isLoadingMore = state.isLoadingMore,
            onLoadMore = viewModel::loadMore,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground)
            .pullRefresh(pullRefreshState),
    ) {
        LazyVerticalGrid(
            columns = if (state.viewMode == ViewMode.Normal) {
                GridCells.Fixed(1)
            } else {
                GridCells.Adaptive(100.dp)
            },
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = listTopPadding,
                bottom = contentPadding.calculateBottomPadding() + 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when {
                state.isLoading && state.movies.isEmpty() -> {
                    items(8) { LoadingCard() }
                }

                state.movies.isEmpty() -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(state.errorMessage)
                    }
                }

                else -> {
                    items(state.movies, key = { it.id }) { movie ->
                        MovieCard(
                            movie = movie,
                            viewMode = state.viewMode,
                            onOpenDetail = { onOpenDetail(movie) },
                            onPlay = { onOpenPlayer(movie, state.movies) },
                            onFavorite = { viewModel.toggleFavorite(movie.id) },
                            onDelete = {
                                if (quickManagementEnabled) {
                                    viewModel.deleteMovie(movie.id)
                                } else {
                                    pendingDeleteMovie = movie
                                }
                            },
                            favoriteEnabled = movie.id !in state.favoriteUpdatingIds,
                            deleteEnabled = movie.id !in state.deletingIds,
                        )
                    }

                    if (state.isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }

        LibraryToolbar(
            state = state,
            onToggleFavoriteFilter = viewModel::cycleFavoriteFilter,
            onToggleViewMode = viewModel::toggleViewMode,
            onToggleOrder = viewModel::toggleSortOrder,
            onSortSelected = viewModel::setSortOption,
            onCyclePageSize = viewModel::cyclePageSize,
            onOpenFilters = { filterDialogVisible = true },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(colors.topBar)
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    bottom = 8.dp,
                ),
        )

        PullRefreshIndicator(
            refreshing = state.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = listTopPadding),
        )

        activeDetailFilterLabel?.let { label ->
            DetailFilterChip(
                label = label,
                onClear = viewModel::clearDetailFilter,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = contentPadding.calculateTopPadding() + 62.dp,
                    ),
            )
        }

        MovieTotalBadge(
            total = state.total,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 14.dp,
                    bottom = contentPadding.calculateBottomPadding() + 18.dp,
                ),
        )

        if (filterDialogVisible) {
            FilterDialog(
                state = state,
                yearOptions = yearOptions,
                monthOptions = monthOptions,
                onDismiss = { filterDialogVisible = false },
                onToggleYear = viewModel::toggleYear,
                onToggleMonth = viewModel::toggleMonth,
                onToggleGenre = viewModel::toggleGenre,
                onClearFilters = viewModel::clearFilters,
            )
        }

        pendingDeleteMovie?.let { movie ->
            DeleteConfirmDialog(
                message = "确定要删除 ${movie.fanhao.ifBlank { movie.name }} 吗？",
                deleting = movie.id in state.deletingIds,
                onDismiss = {
                    if (movie.id !in state.deletingIds) pendingDeleteMovie = null
                },
                onConfirm = {
                    viewModel.deleteMovie(movie.id)
                },
            )
        }
    }
}

@Composable
private fun MovieTotalBadge(
    total: Int,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceOverlay,
    ) {
        Text(
            text = "共 $total 部影片",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = colors.textPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun DetailFilterChip(
    label: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceOverlay,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = colors.textPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                modifier = Modifier.size(22.dp),
                shape = RoundedCornerShape(8.dp),
                color = colors.surfaceVariant,
                onClick = onClear,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "清空详情筛选",
                        modifier = Modifier.size(14.dp),
                        tint = colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryToolbar(
    state: LibraryUiState,
    onToggleFavoriteFilter: () -> Unit,
    onToggleViewMode: () -> Unit,
    onToggleOrder: () -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onCyclePageSize: () -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ToolbarActionButton(
            modifier = Modifier.weight(1f),
            icon = when (state.favoriteFilter) {
                FavoriteFilterMode.All -> Icons.Rounded.FavoriteBorder
                FavoriteFilterMode.Favorite -> Icons.Rounded.Favorite
                FavoriteFilterMode.Unfavorite -> Icons.Rounded.FavoriteBorder
            },
            contentDescription = when (state.favoriteFilter) {
                FavoriteFilterMode.All -> "显示全部"
                FavoriteFilterMode.Favorite -> "仅看已收藏"
                FavoriteFilterMode.Unfavorite -> "仅看未收藏"
            },
            onClick = onToggleFavoriteFilter,
            containerColor = when (state.favoriteFilter) {
                FavoriteFilterMode.All -> colors.surfaceVariant
                FavoriteFilterMode.Favorite -> colors.dangerSoft
                FavoriteFilterMode.Unfavorite -> colors.dangerSoft
            },
            contentColor = when (state.favoriteFilter) {
                FavoriteFilterMode.All -> colors.textPrimary
                FavoriteFilterMode.Favorite -> colors.danger
                FavoriteFilterMode.Unfavorite -> colors.danger
            },
        )

        Box(modifier = Modifier.weight(1f)) {
            ToolbarActionButton(
                modifier = Modifier.fillMaxWidth(),
                icon = when (state.sortOption) {
                    SortOption.ReleaseDate -> Icons.Rounded.Schedule
                    SortOption.CreatedAt -> Icons.AutoMirrored.Rounded.Sort
                    SortOption.FavoriteAt -> Icons.Rounded.Favorite
                    SortOption.Fanhao -> Icons.Rounded.SortByAlpha
                },
                contentDescription = "排序字段",
                onClick = { sortMenuExpanded = true },
            )
            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
            ) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.toChineseLabel()) },
                        onClick = {
                            sortMenuExpanded = false
                            onSortSelected(option)
                        },
                    )
                }
            }
        }

        ToolbarTextButton(
            modifier = Modifier.weight(1f),
            text = state.limit.toString(),
            onClick = onCyclePageSize,
        )

        ToolbarActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.SwapVert,
            contentDescription = "正倒序",
            onClick = onToggleOrder,
            containerColor = if (state.sortOrder == "desc") colors.accentSoft else colors.surfaceVariant,
        )

        ToolbarActionButton(
            modifier = Modifier.weight(1f),
            icon = if (state.viewMode == ViewMode.Normal) Icons.Rounded.GridView else Icons.Rounded.ViewStream,
            contentDescription = "视图模式",
            onClick = onToggleViewMode,
        )

        ToolbarActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.FilterAlt,
            contentDescription = "筛选",
            onClick = onOpenFilters,
            containerColor = if (
                state.selectedGenres.isNotEmpty() ||
                state.selectedYear != null ||
                state.selectedMonth != null ||
                state.selectedActress != null ||
                state.selectedStudio != null ||
                state.selectedPublisher != null ||
                state.selectedSeries != null ||
                state.selectedFanhaoSeries != null
            ) {
                colors.accentSoft
            } else {
                colors.surfaceVariant
            },
        )
    }
}

@Composable
private fun ToolbarActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
) {
    val colors = AppTheme.colors
    val resolvedContainerColor = if (containerColor == Color.Unspecified) colors.surfaceVariant else containerColor
    val resolvedContentColor = if (contentColor == Color.Unspecified) colors.textPrimary else contentColor
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = resolvedContainerColor,
            contentColor = resolvedContentColor,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ToolbarTextButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = colors.surfaceVariant,
            contentColor = colors.textPrimary,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterDialog(
    state: LibraryUiState,
    yearOptions: List<String>,
    monthOptions: List<String>,
    onDismiss: () -> Unit,
    onToggleYear: (String) -> Unit,
    onToggleMonth: (String) -> Unit,
    onToggleGenre: (String) -> Unit,
    onClearFilters: () -> Unit,
) {
    val colors = AppTheme.colors
    val selectedFilterCount = state.selectedGenres.size +
        (if (state.selectedYear != null) 1 else 0) +
        (if (state.selectedMonth != null) 1 else 0) +
        (if (state.selectedActress != null) 1 else 0) +
        (if (state.selectedStudio != null) 1 else 0) +
        (if (state.selectedPublisher != null) 1 else 0) +
        (if (state.selectedSeries != null) 1 else 0) +
        (if (state.selectedFanhaoSeries != null) 1 else 0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.surfaceOverlay)
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "筛选",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = if (selectedFilterCount > 0) {
                                    "已选择 $selectedFilterCount 项"
                                } else {
                                    "按年份、月份、类别筛选"
                                },
                                color = colors.textTertiary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        TextButton(onClick = onDismiss) {
                            Text("关闭")
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterSection(
                            title = "年份",
                            options = yearOptions,
                            selected = state.selectedYear?.let(::setOf) ?: emptySet(),
                            onToggle = onToggleYear,
                        )
                        FilterSection(
                            title = "月份",
                            options = monthOptions,
                            selected = state.selectedMonth?.let(::setOf) ?: emptySet(),
                            onToggle = onToggleMonth,
                        )
                        if (state.genres.isNotEmpty()) {
                            FilterSection(
                                title = "类别",
                                options = state.genres.map { it.name },
                                selected = state.selectedGenres,
                                onToggle = onToggleGenre,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onClearFilters,
                            enabled = selectedFilterCount > 0,
                        ) {
                            Text("清空已选")
                        }
                        Button(onClick = onDismiss) {
                            Text("完成")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    val colors = AppTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                options.forEach { option ->
                    Surface(
                        color = if (selected.contains(option)) colors.accentSoft else colors.surfaceVariant,
                        shape = RoundedCornerShape(999.dp),
                        onClick = { onToggle(option) },
                    ) {
                        Text(
                            text = option,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = if (selected.contains(option)) colors.accentText else colors.textPrimary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieCard(
    movie: LibraryMovie,
    viewMode: ViewMode,
    onOpenDetail: () -> Unit,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    favoriteEnabled: Boolean,
    deleteEnabled: Boolean,
) {
    val colors = AppTheme.colors
    val fanhaoTextColor = Color(0xFFE0E7FF)
    val compact = viewMode == ViewMode.Compact

    Card(
        shape = RoundedCornerShape(if (compact) 12.dp else 10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenDetail)
//                    .height(if (compact) 228.dp else 196.dp),
            ) {
                if (!movie.cover.isNullOrBlank()) {
                    AsyncImage(
                        model = movie.cover,
                        contentDescription = movie.name,
                        contentScale = ContentScale.Crop,
                        alignment = if (compact) Alignment.CenterEnd else Alignment.Center,
                        modifier = Modifier.fillMaxWidth().aspectRatio(if (compact) 140f / 200f else 800f / 536f),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth().aspectRatio(147f / 200f)
                            .background(colors.topBar),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "暂无封面", color = colors.textTertiary)
                    }
                }
                if (compact) {
                    Text(
                        text = movie.fanhao,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .clip(RoundedCornerShape(0.dp, 8.dp, 0.dp, 0.dp))
                            .background(colors.accent.copy(alpha = 0.75f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        color = fanhaoTextColor,
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    Text(
                        text = movie.fanhao,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.accent)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        color = fanhaoTextColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }


            }

            Column(
                modifier = Modifier
                    .clickable(onClick = onOpenDetail)
                    .padding(if (compact) 4.dp else 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = movie.name,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Normal,
                    fontSize = if (compact) 12.sp else 14.sp,
                    lineHeight = if (compact) 14.sp else 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ItemActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.PlayArrow,
                        text = "播放",
                        showText = !compact,
                        onClick = onPlay,
                    )
                    ItemActionButton(
                        modifier = Modifier.weight(1f),
                        icon = if (movie.isFavorite == 1) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        text = "收藏",
                        showText = !compact,
                        onClick = onFavorite,
                        enabled = favoriteEnabled,
                    )
                    ItemActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Delete,
                        text = "删除",
                        showText = !compact,
                        onClick = onDelete,
                        containerColor = colors.dangerSoft,
                        enabled = deleteEnabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    showText: Boolean,
    onClick: () -> Unit,
    containerColor: Color = Color.Unspecified,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val resolvedContainerColor = if (containerColor == Color.Unspecified) colors.surfaceVariant else containerColor
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = resolvedContainerColor,
            contentColor = colors.textPrimary,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(16.dp),
        )
        if (showText) {
            Text(
                text = text,
                modifier = Modifier.padding(start = 4.dp),
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun LoadingCard() {
    val colors = AppTheme.colors
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.borderSubtle),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(colors.surfaceVariant),
            )
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth(0.7f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.placeholderSurface),
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 10.dp)
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceVariant),
            )
        }
    }
}

@Composable
private fun EmptyState(errorMessage: String?) {
    val colors = AppTheme.colors
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "没有找到影片",
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = errorMessage ?: "请调整筛选条件，或检查接口服务地址是否正确。",
                color = colors.textTertiary,
            )
        }
    }
}

private suspend fun LazyGridState.loadMoreWhenNeeded(
    itemCount: Int,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
) {
    snapshotFlow {
        layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    }.collect { lastVisible: Int ->
        val triggerIndex = itemCount - 6
        if (hasMore && !isLoadingMore && itemCount > 0 && lastVisible >= triggerIndex) {
            onLoadMore()
        }
    }
}

private fun SortOption.toChineseLabel(): String {
    return when (this) {
        SortOption.ReleaseDate -> "发行日期"
        SortOption.CreatedAt -> "收录时间"
        SortOption.FavoriteAt -> "收藏时间"
        SortOption.Fanhao -> "番号"
    }
}

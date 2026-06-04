package com.zhumeng.fake115.ui.netdisk

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredGridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.zhumeng.fake115.NetDiskDetailActivity
import com.zhumeng.fake115.PlayerActivity
import com.zhumeng.fake115.data.AppSettings
import com.zhumeng.fake115.data.model.FavoriteFilterMode
import com.zhumeng.fake115.data.model.NetDiskFile
import com.zhumeng.fake115.data.model.NetDiskPathNode
import com.zhumeng.fake115.ui.common.DeleteConfirmDialog
import com.zhumeng.fake115.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private const val LABEL_ROOT = "根目录"
private const val LABEL_EMPTY = "当前目录没有文件"
private const val LABEL_EMPTY_HINT = "下拉刷新，或者进入其他目录再试试。"
private const val LABEL_UNNAMED = "未命名文件"
private const val LABEL_FOLDER = "文件夹"
private const val LABEL_VIDEO = "视频"
private const val LABEL_FILE = "文件"
private const val LABEL_PAGE_CHOSEN = "已选择第%d页"
private const val PAGE_BUTTON_INTERVAL_MS = 1000L

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NetDiskScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: NetDiskViewModel = viewModel(),
) {
    val colors = AppTheme.colors
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val gridState = rememberLazyStaggeredGridState()
    val context = LocalContext.current
    val quickManagementEnabled by AppSettings.quickManagementEnabledFlow(context).collectAsState()
    var activeToast by remember { mutableStateOf<Toast?>(null) }
    var lastPageButtonClickAt by remember { mutableLongStateOf(0L) }
    var pageButtonLocked by remember { mutableStateOf(false) }
    var pendingDeleteFile by remember { mutableStateOf<NetDiskFile?>(null) }
    val scrollScope = rememberCoroutineScope()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
    )

    BackHandler(enabled = state.currentCid != "0") {
        viewModel.navigateUp()
    }

    LaunchedEffect(listState, state.viewMode, state.files.size, state.hasMore, state.isLoadingMore) {
        if (state.viewMode == NetDiskViewMode.List) {
            listState.loadMoreWhenNeeded(
                itemCount = state.files.size,
                hasMore = state.hasMore,
                isLoadingMore = state.isLoadingMore,
                onLoadMore = viewModel::loadMore,
            )
        }
    }

    LaunchedEffect(gridState, state.viewMode, state.files.size, state.hasMore, state.isLoadingMore) {
        if (state.viewMode == NetDiskViewMode.Waterfall) {
            gridState.loadMoreWhenNeeded(
                itemCount = state.files.size,
                hasMore = state.hasMore,
                isLoadingMore = state.isLoadingMore,
                onLoadMore = viewModel::loadMore,
            )
        }
    }

    LaunchedEffect(state.contentResetVersion) {
        if (state.contentResetVersion > 0) {
            listState.scrollToItem(0)
            gridState.scrollToItem(0)
        }
    }

    LaunchedEffect(state.isLoading, state.isRefreshing, state.isLoadingMore) {
        if (!state.isLoading && !state.isRefreshing && !state.isLoadingMore) {
            pageButtonLocked = false
        }
    }

    LaunchedEffect(state.deletingIds, state.rawFiles, pendingDeleteFile) {
        val file = pendingDeleteFile ?: return@LaunchedEffect
        if (file.id !in state.deletingIds && state.rawFiles.none { it.id == file.id }) {
            pendingDeleteFile = null
        }
    }

    fun showPageToast(page: Int) {
        activeToast?.cancel()
        activeToast = Toast.makeText(
            context,
            LABEL_PAGE_CHOSEN.format(page),
            Toast.LENGTH_SHORT,
        ).also { it.show() }
    }

    fun handlePagedAction(action: () -> Int?) {
        if (pageButtonLocked) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastPageButtonClickAt < PAGE_BUTTON_INTERVAL_MS) return
        lastPageButtonClickAt = now
        action()?.let { page ->
            pageButtonLocked = true
            showPageToast(page)
        }
    }

    fun scrollToTop() {
        scrollScope.launch {
            if (state.viewMode == NetDiskViewMode.List) {
                listState.scrollToItem(0)
            } else {
                gridState.scrollToItem(0)
            }
        }
    }

    fun scrollToBottom() {
        if (state.files.isEmpty()) return
        scrollScope.launch {
            val lastIndex = state.files.lastIndex
            if (state.viewMode == NetDiskViewMode.List) {
                listState.scrollToItem(lastIndex)
            } else {
                gridState.scrollToItem(lastIndex)
            }
        }
    }

    fun openFile(file: NetDiskFile) {
        if (file.isVideo && !file.pc.isNullOrBlank()) {
            val playlist = state.files.filter {
                it.isVideo && !it.pc.isNullOrBlank()
            }
            val playerIntent = PlayerActivity.createNetDiskIntent(
                context = context,
                fileId = file.id,
                title = file.n,
                deleteLabel = file.n,
                pc = file.pc,
                isFavorite = file.isStarred,
                playlist = playlist,
                removeFromPlaylistOnFavorite = state.favoriteFilter == FavoriteFilterMode.Unfavorite,
            )
            runCatching {
                context.startActivity(playerIntent)
            }.onFailure {
                context.startActivity(
                    PlayerActivity.createNetDiskIntent(
                        context = context,
                        fileId = file.id,
                        title = file.n,
                        deleteLabel = file.n,
                        pc = file.pc,
                        isFavorite = file.isStarred,
                        removeFromPlaylistOnFavorite = state.favoriteFilter == FavoriteFilterMode.Unfavorite,
                    )
                )
            }
        } else if (file.isDirectory) {
            viewModel.openDirectory(file)
        }
    }

    fun openDetail(file: NetDiskFile) {
        context.startActivity(
            NetDiskDetailActivity.createIntent(
                context = context,
                id = file.id,
                title = file.n,
                isDirectory = file.isDirectory,
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground)
            .pullRefresh(pullRefreshState),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.topBar),
            ) {
                NetDiskControls(
                    state = state,
                    onToggleVideoFilter = viewModel::toggleVideoFilter,
                    onToggleFavoriteFilter = viewModel::cycleFavoriteFilter,
                    onSortSelected = viewModel::setSortOption,
                    onToggleOrder = viewModel::toggleSortOrder,
                    onCyclePageSize = viewModel::cyclePageSize,
                    onToggleViewMode = viewModel::toggleViewMode,
                )

                NetDiskToolbar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 6.dp),
                    path = state.path,
                    onOpenPath = viewModel::openPath,
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (state.viewMode == NetDiskViewMode.List) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 0.dp,
                            bottom = contentPadding.calculateBottomPadding() + 74.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        when {
                            state.isLoading && state.files.isEmpty() -> {
                                items(6) { NetDiskLoadingCard() }
                            }

                            state.files.isEmpty() -> {
                                item { NetDiskEmptyState(errorMessage = state.errorMessage) }
                            }

                            else -> {
                                items(state.files, key = { "${it.id}_${it.parentId}" }) { file ->
                                    NetDiskFileCard(
                                        file = file,
                                        onOpen = { openFile(file) },
                                        onOpenDetail = { openDetail(file) },
                                    )
                                }

                                if (state.isLoadingMore) {
                                    item { NetDiskLoadingMoreIndicator() }
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(156.dp),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 0.dp,
                            bottom = contentPadding.calculateBottomPadding() + 74.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalItemSpacing = 10.dp,
                    ) {
                        when {
                            state.isLoading && state.files.isEmpty() -> {
                                items(8) { NetDiskWaterfallLoadingCard() }
                            }

                            state.files.isEmpty() -> {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    NetDiskEmptyState(errorMessage = state.errorMessage)
                                }
                            }

                            else -> {
                                staggeredGridItems(state.files, key = { "${it.id}_${it.parentId}" }) { file ->
                                    NetDiskWaterfallFileCard(
                                        file = file,
                                        favoriteUpdating = file.id in state.starUpdatingIds,
                                        deleting = file.id in state.deletingIds,
                                        onOpen = { openFile(file) },
                                        onToggleFavorite = { viewModel.toggleFileStar(file.id) },
                                        onDelete = {
                                            if (quickManagementEnabled) {
                                                viewModel.deleteFile(file.id)
                                            } else {
                                                pendingDeleteFile = file
                                            }
                                        },
                                        onOpenDetail = { openDetail(file) },
                                    )
                                }

                                if (state.isLoadingMore) {
                                    item(span = StaggeredGridItemSpan.FullLine) {
                                        NetDiskLoadingMoreIndicator()
                                    }
                                }
                            }
                        }
                    }
                }

                PullRefreshIndicator(
                    refreshing = state.isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }

        NetDiskPagerFloatingControls(
            state = state,
            pageButtonsEnabled = !pageButtonLocked,
            onPreviousPage = { handlePagedAction(viewModel::previousPage) },
            onGoToPage = { page -> handlePagedAction { viewModel.goToPage(page) } },
            onNextPage = { handlePagedAction(viewModel::nextPage) },
            onLoadAll = viewModel::loadAllFiles,
            onScrollToTop = ::scrollToTop,
            onScrollToBottom = ::scrollToBottom,
            onToggleDurationSort = viewModel::toggleDurationSort,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = contentPadding.calculateBottomPadding() + 8.dp,
                ),
        )

        pendingDeleteFile?.let { file ->
            DeleteConfirmDialog(
                message = "确定要删除 ${file.n.ifBlank { LABEL_UNNAMED }} 吗？",
                deleting = file.id in state.deletingIds,
                onDismiss = {
                    if (file.id !in state.deletingIds) pendingDeleteFile = null
                },
                onConfirm = {
                    viewModel.deleteFile(file.id)
                },
            )
        }
    }
}

@Composable
private fun NetDiskControls(
    state: NetDiskUiState,
    onToggleVideoFilter: () -> Unit,
    onToggleFavoriteFilter: () -> Unit,
    onSortSelected: (NetDiskSortOption) -> Unit,
    onToggleOrder: () -> Unit,
    onCyclePageSize: () -> Unit,
    onToggleViewMode: () -> Unit,
) {
    val colors = AppTheme.colors
    var sortMenuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NetDiskToolbarActionButton(
            modifier = Modifier.weight(1f),
            icon = when (state.favoriteFilter) {
                FavoriteFilterMode.All -> Icons.Rounded.FavoriteBorder
                FavoriteFilterMode.Favorite -> Icons.Rounded.Favorite
                FavoriteFilterMode.Unfavorite -> Icons.Rounded.FavoriteBorder
            },
            contentDescription = when (state.favoriteFilter) {
                FavoriteFilterMode.All -> "显示全部文件"
                FavoriteFilterMode.Favorite -> "仅看已收藏文件"
                FavoriteFilterMode.Unfavorite -> "仅看未收藏文件"
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
        NetDiskToolbarActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.VideoLibrary,
            contentDescription = "全部/仅视频",
            onClick = onToggleVideoFilter,
            containerColor = if (state.onlyVideos) colors.accentSoft else colors.surfaceVariant,
            contentColor = if (state.onlyVideos) colors.accentText else colors.textPrimary,
        )
        Box(modifier = Modifier.weight(1f)) {
            NetDiskToolbarActionButton(
                modifier = Modifier.fillMaxWidth(),
                icon = state.sortOption.icon(),
                contentDescription = "排序字段",
                onClick = { sortMenuExpanded = true },
            )
            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
            ) {
                NetDiskSortOption.entries.forEach { option ->
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
        NetDiskToolbarActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.SwapVert,
            contentDescription = "正倒序",
            onClick = onToggleOrder,
            containerColor = if (state.isAscending) colors.accentSoft else colors.surfaceVariant,
        )
        NetDiskToolbarTextButton(
            modifier = Modifier.weight(1.2f),
            text = state.limit.toString(),
            onClick = onCyclePageSize,
        )
        NetDiskToolbarActionButton(
            modifier = Modifier.weight(1f),
            icon = when (state.viewMode) {
                NetDiskViewMode.List -> Icons.Rounded.GridView
                NetDiskViewMode.Waterfall -> Icons.Rounded.ViewAgenda
            },
            contentDescription = "切换视图",
            onClick = onToggleViewMode,
            containerColor = if (state.viewMode == NetDiskViewMode.Waterfall) {
                colors.accentSoft
            } else {
                colors.surfaceVariant
            },
            contentColor = if (state.viewMode == NetDiskViewMode.Waterfall) {
                colors.accentText
            } else {
                colors.textPrimary
            },
        )
    }
}

@Composable
private fun NetDiskPagerFloatingControls(
    state: NetDiskUiState,
    pageButtonsEnabled: Boolean,
    onPreviousPage: () -> Unit,
    onGoToPage: (Int) -> Unit,
    onNextPage: () -> Unit,
    onLoadAll: () -> Unit,
    onScrollToTop: () -> Unit,
    onScrollToBottom: () -> Unit,
    onToggleDurationSort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val maxMenuHeight = LocalConfiguration.current.screenHeightDp.dp * 0.5f
    var pageMenuExpanded by remember { mutableStateOf(false) }
    val busy = state.isLoading || state.isRefreshing || state.isLoadingMore || state.isLoadingAll
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceOverlay.copy(alpha = 0.94f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NetDiskFloatingIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "上一页",
                onClick = onPreviousPage,
                enabled = pageButtonsEnabled && state.hasPreviousPage && !busy,
            )
            Box {
                NetDiskFloatingTextButton(
                    text = state.currentPage.toString(),
                    contentDescription = "选择页码",
                    onClick = { pageMenuExpanded = true },
                    enabled = !busy && state.totalPages > 1,
                )
                DropdownMenu(
                    expanded = pageMenuExpanded,
                    onDismissRequest = { pageMenuExpanded = false },
                    modifier = Modifier
                        .width(58.dp)
                        .heightIn(max = maxMenuHeight),
                    containerColor = colors.elevatedSurface.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    repeat(state.totalPages) { index ->
                        val page = index + 1
                        Text(
                            text = page.toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Transparent)
                                .clickable {
                                    pageMenuExpanded = false
                                    onGoToPage(page)
                                }
                                .padding(horizontal = 6.dp, vertical = 9.dp),
                            color = if (page == state.currentPage) colors.accentText else colors.textPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (page == state.currentPage) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
            NetDiskFloatingIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "下一页",
                onClick = onNextPage,
                enabled = pageButtonsEnabled && state.hasNextPage && !busy,
            )
            NetDiskFloatingIconButton(
                icon = Icons.Rounded.PlaylistAdd,
                contentDescription = "加载全部",
                onClick = onLoadAll,
                enabled = !busy,
            )
            NetDiskFloatingIconButton(
                icon = Icons.Rounded.Upload,
                contentDescription = "返回顶部",
                onClick = onScrollToTop,
                enabled = state.files.isNotEmpty(),
            )
            NetDiskFloatingIconButton(
                icon = Icons.Rounded.Download,
                contentDescription = "返回底部",
                onClick = onScrollToBottom,
                enabled = state.files.isNotEmpty(),
            )
            if (state.onlyVideos) {
                val durationSortContentColor = when (state.durationSortOrder) {
                    NetDiskDurationSortOrder.Asc -> Color(0xFF7EE0B5)
                    NetDiskDurationSortOrder.Desc -> Color(0xFFFFC857)
                    null -> colors.textTertiary
                }
                val durationSortDescription = when (state.durationSortOrder) {
                    NetDiskDurationSortOrder.Asc -> "按时长正序"
                    NetDiskDurationSortOrder.Desc -> "按时长倒序"
                    null -> "不按时长排序"
                }
                NetDiskFloatingIconButton(
                    icon = Icons.Rounded.Schedule,
                    contentDescription = durationSortDescription,
                    onClick = onToggleDurationSort,
                    enabled = !busy,
                    contentColor = durationSortContentColor,
                )
            }
            NetDiskFloatingTextBadge(text = "共 ${state.files.count { it.isVideo }}")
        }
    }
}
@Composable
private fun NetDiskFloatingIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentColor: Color = Color.Unspecified,
) {
    val colors = AppTheme.colors
    val resolvedContentColor = if (contentColor == Color.Unspecified) colors.textPrimary else contentColor
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) resolvedContentColor else colors.textTertiary.copy(alpha = 0.45f),
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun NetDiskFloatingTextButton(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width = 44.dp, height = 36.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color.Transparent,
            contentColor = colors.textPrimary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = colors.textTertiary.copy(alpha = 0.45f),
        ),
    ) {
        Text(
            text = text,
            maxLines = 1,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) colors.textPrimary else colors.textTertiary.copy(alpha = 0.45f),
        )
    }
}

@Composable
private fun NetDiskFloatingTextBadge(
    text: String,
) {
    val colors = AppTheme.colors
    Text(
        text = text,
        modifier = Modifier.padding(start = 4.dp, end = 8.dp),
        color = colors.textPrimary,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        maxLines = 1,
    )
}

@Composable
private fun NetDiskVideoTotalBadge(
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
private fun NetDiskToolbarActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
) {
    val colors = AppTheme.colors
    val resolvedContainerColor = if (containerColor == Color.Unspecified) colors.surfaceVariant else containerColor
    val resolvedContentColor = if (contentColor == Color.Unspecified) colors.textPrimary else contentColor
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = resolvedContainerColor,
            contentColor = resolvedContentColor,
            disabledContainerColor = resolvedContainerColor.copy(alpha = 0.45f),
            disabledContentColor = resolvedContentColor.copy(alpha = 0.45f),
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
private fun NetDiskToolbarTextButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = colors.surfaceVariant,
            contentColor = colors.textPrimary,
        ),
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NetDiskToolbar(
    modifier: Modifier = Modifier,
    path: List<NetDiskPathNode>,
    onOpenPath: (String) -> Unit,
) {
    val colors = AppTheme.colors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (path.isEmpty()) {
                PathText(name = LABEL_ROOT, onClick = { onOpenPath("0") })
            } else {
                path.forEachIndexed { index, node ->
                    PathText(
                        name = node.name,
                        onClick = { onOpenPath(node.cid.ifBlank { "0" }) },
                    )
                    if (index < path.lastIndex) {
                        Text(
                            text = "/",
                            color = colors.textTertiary,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PathText(
    name: String,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Text(
        text = name,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 1.dp, vertical = 1.dp),
        color = colors.textPrimary,
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun NetDiskFileCard(
    file: NetDiskFile,
    onOpen: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    val colors = AppTheme.colors
    Card(
        modifier = Modifier.clickable(onClick = onOpen),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NetDiskFileLeadingVisual(file = file)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = file.n.ifBlank { LABEL_UNNAMED },
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = file.uploadTime?.let(::formatListTime) ?: "-",
                        modifier = Modifier.weight(1f),
                        color = colors.textTertiary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (file.isVideo) {
                        Text(
                            text = formatVideoDuration(file.durationSeconds),
                            color = colors.textTertiary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!file.isDirectory) {
                        Text(
                            text = formatFileSize(file.size),
                            color = colors.textTertiary,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (file.isStarred) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = "已收藏",
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(20.dp),
                )
            }

            IconButton(
                onClick = onOpenDetail,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "详情",
                    tint = colors.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun NetDiskWaterfallFileCard(
    file: NetDiskFile,
    favoriteUpdating: Boolean,
    deleting: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    val colors = AppTheme.colors
    Card(
        modifier = Modifier.clickable(onClick = onOpen),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(Color.Black),
            ) {
                val thumbnail = file.thumbnail?.cleanThumbnailUrl()?.takeIf { file.isVideo && it.isUrlLike() }
                if (thumbnail != null) {
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = file.typeLabel(),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = file.typeIcon(),
                            contentDescription = file.typeLabel(),
                            tint = file.typeTint(),
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }

                if (file.isVideo) {
                    NetDiskMediaOverlayLabel(
                        text = formatVideoDuration(file.durationSeconds),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = file.n.ifBlank { LABEL_UNNAMED },
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = file.uploadTime?.let(::formatListTime) ?: "-",
                    color = colors.textTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NetDiskCardIconButton(
                        modifier = Modifier.weight(1f),
                        icon = if (file.isStarred) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (file.isStarred) "取消收藏" else "收藏",
                        onClick = onToggleFavorite,
                        enabled = !favoriteUpdating,
                        containerColor = if (file.isStarred) colors.dangerSoft else colors.surfaceVariant,
                        contentColor = if (file.isStarred) colors.danger else colors.textPrimary,
                    )
                    NetDiskCardIconButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Delete,
                        contentDescription = "删除",
                        onClick = onDelete,
                        enabled = !deleting,
                        containerColor = colors.dangerSoft,
                        contentColor = colors.textPrimary,
                    )
                    NetDiskCardIconButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Info,
                        contentDescription = "详情",
                        onClick = onOpenDetail,
                        containerColor = colors.surfaceVariant,
                        contentColor = colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun NetDiskMediaOverlayLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.58f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun NetDiskCardIconButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
) {
    val colors = AppTheme.colors
    val resolvedContainerColor = if (containerColor == Color.Unspecified) colors.surfaceVariant else containerColor
    val resolvedContentColor = if (contentColor == Color.Unspecified) colors.textPrimary else contentColor
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = resolvedContainerColor,
            contentColor = resolvedContentColor,
            disabledContainerColor = resolvedContainerColor.copy(alpha = 0.45f),
            disabledContentColor = resolvedContentColor.copy(alpha = 0.45f),
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun NetDiskFileLeadingVisual(
    file: NetDiskFile,
) {
    val thumbnail = file.thumbnail?.cleanThumbnailUrl()?.takeIf { file.isVideo && it.isUrlLike() }
    if (thumbnail != null) {
        AsyncImage(
            model = thumbnail,
            contentDescription = file.typeLabel(),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 42.dp, height = 28.dp)
                .background(Color.Black, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp)),
        )
    } else {
        Icon(
            imageVector = file.typeIcon(),
            contentDescription = file.typeLabel(),
            tint = file.typeTint(),
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun NetDiskLoadingCard() {
    val colors = AppTheme.colors
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(16.dp)
                    .background(colors.placeholderSurface, RoundedCornerShape(999.dp)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.32f)
                    .height(12.dp)
                    .background(colors.surfaceVariant, RoundedCornerShape(999.dp)),
            )
        }
    }
}

@Composable
private fun NetDiskWaterfallLoadingCard() {
    val colors = AppTheme.colors
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(colors.placeholderSurface),
            )
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .height(14.dp)
                        .background(colors.placeholderSurface, RoundedCornerShape(999.dp)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.56f)
                        .height(12.dp)
                        .background(colors.surfaceVariant, RoundedCornerShape(999.dp)),
                )
                Box(
                    modifier = Modifier
                        .width(78.dp)
                        .height(24.dp)
                        .background(colors.surfaceVariant, RoundedCornerShape(999.dp)),
                )
            }
        }
    }
}

@Composable
private fun NetDiskLoadingMoreIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NetDiskEmptyState(errorMessage: String?) {
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
                text = LABEL_EMPTY,
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = errorMessage ?: LABEL_EMPTY_HINT,
                color = colors.textTertiary,
            )
        }
    }
}

private fun NetDiskSortOption.icon(): ImageVector {
    return when (this) {
        NetDiskSortOption.UpdateTime -> Icons.Rounded.AccessTime
        NetDiskSortOption.FileName -> Icons.Rounded.SortByAlpha
        NetDiskSortOption.FileSize -> Icons.Rounded.Storage
        NetDiskSortOption.FileType -> Icons.AutoMirrored.Rounded.Sort
    }
}

private fun NetDiskSortOption.toChineseLabel(): String {
    return when (this) {
        NetDiskSortOption.UpdateTime -> "更新时间"
        NetDiskSortOption.FileName -> "文件名"
        NetDiskSortOption.FileSize -> "文件大小"
        NetDiskSortOption.FileType -> "文件类型"
    }
}

private fun NetDiskFile.typeLabel(): String {
    if (isDirectory) return LABEL_FOLDER
    if (isVideo) return LABEL_VIDEO
    return suffix?.uppercase(Locale.getDefault()) ?: fileType?.uppercase(Locale.getDefault()) ?: LABEL_FILE
}

private fun NetDiskFile.typeIcon(): ImageVector {
    if (isDirectory) return Icons.Rounded.Folder
    return when {
        isVideo -> Icons.Rounded.VideoLibrary
        suffix.equals("jpg", ignoreCase = true) ||
            suffix.equals("jpeg", ignoreCase = true) ||
            suffix.equals("png", ignoreCase = true) ||
            suffix.equals("gif", ignoreCase = true) ||
            suffix.equals("webp", ignoreCase = true) -> Icons.Rounded.Image
        suffix.equals("mp3", ignoreCase = true) ||
            suffix.equals("flac", ignoreCase = true) ||
            suffix.equals("wav", ignoreCase = true) -> Icons.Rounded.MusicNote
        suffix.equals("pdf", ignoreCase = true) ||
            suffix.equals("doc", ignoreCase = true) ||
            suffix.equals("docx", ignoreCase = true) ||
            suffix.equals("txt", ignoreCase = true) -> Icons.Rounded.Description
        suffix.equals("mp4", ignoreCase = true) ||
            suffix.equals("mkv", ignoreCase = true) ||
            suffix.equals("avi", ignoreCase = true) -> Icons.Rounded.Movie
        else -> Icons.Rounded.InsertDriveFile
    }
}

private fun NetDiskFile.typeTint(): Color {
    return when {
        isDirectory -> Color(0xFF8CB4FF)
        isVideo -> Color(0xFFFF7A7A)
        suffix.equals("jpg", ignoreCase = true) ||
            suffix.equals("jpeg", ignoreCase = true) ||
            suffix.equals("png", ignoreCase = true) ||
            suffix.equals("gif", ignoreCase = true) ||
            suffix.equals("webp", ignoreCase = true) -> Color(0xFFFFC857)
        suffix.equals("mp3", ignoreCase = true) ||
            suffix.equals("flac", ignoreCase = true) ||
            suffix.equals("wav", ignoreCase = true) -> Color(0xFF7EE0B5)
        else -> Color(0xFFB7C3D9)
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return runCatching {
        val millis = if (timestamp < 10_000_000_000L) timestamp * 1000 else timestamp
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
    }.getOrDefault(timestamp.toString())
}

private fun formatListTime(timestamp: Long): String {
    return runCatching {
        val zoneId = ZoneId.systemDefault()
        val millis = if (timestamp < 10_000_000_000L) timestamp * 1000 else timestamp
        val dateTime = Instant.ofEpochMilli(millis).atZone(zoneId)
        val now = Instant.now().atZone(zoneId)
        val formatter = when {
            dateTime.toLocalDate() == now.toLocalDate() -> DateTimeFormatter.ofPattern("HH:mm")
            dateTime.year == now.year -> DateTimeFormatter.ofPattern("MM-dd HH:mm")
            else -> DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        }
        dateTime.format(formatter)
    }.getOrDefault(formatTimestamp(timestamp))
}

private fun formatFileSize(size: Long): String {
    if (size <= 0L) return "-"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = size.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) {
        "${value.toLong()} ${units[unitIndex]}"
    } else {
        String.format(Locale.getDefault(), "%.2f %s", value, units[unitIndex])
    }
}

private fun formatVideoDuration(durationSeconds: Long?): String {
    val totalSeconds = durationSeconds?.coerceAtLeast(0L) ?: 0L
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

private fun String.isUrlLike(): Boolean {
    return startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
}

private fun String.cleanThumbnailUrl(): String {
    return substringBefore("?")
}

private suspend fun LazyListState.loadMoreWhenNeeded(
    itemCount: Int,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
) {
    snapshotFlow {
        layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    }.collect { lastVisible ->
        val triggerIndex = itemCount - 6
        if (hasMore && !isLoadingMore && itemCount > 0 && lastVisible >= triggerIndex) {
            onLoadMore()
        }
    }
}

private suspend fun LazyStaggeredGridState.loadMoreWhenNeeded(
    itemCount: Int,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
) {
    snapshotFlow {
        layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: 0
    }.collect { lastVisible ->
        val triggerIndex = itemCount - 8
        if (hasMore && !isLoadingMore && itemCount > 0 && lastVisible >= triggerIndex) {
            onLoadMore()
        }
    }
}

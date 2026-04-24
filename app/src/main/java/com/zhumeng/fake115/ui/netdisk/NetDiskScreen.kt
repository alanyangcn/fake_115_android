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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhumeng.fake115.NetDiskDetailActivity
import com.zhumeng.fake115.PlayerActivity
import com.zhumeng.fake115.data.model.NetDiskFile
import com.zhumeng.fake115.data.model.NetDiskPathNode
import com.zhumeng.fake115.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private const val LABEL_ROOT = "\u6839\u76ee\u5f55"
private const val LABEL_EMPTY = "\u5f53\u524d\u76ee\u5f55\u6ca1\u6709\u6587\u4ef6"
private const val LABEL_EMPTY_HINT = "\u4e0b\u62c9\u5237\u65b0\uff0c\u6216\u8005\u8fdb\u5165\u5176\u4ed6\u76ee\u5f55\u518d\u8bd5\u8bd5\u3002"
private const val LABEL_UNNAMED = "\u672a\u547d\u540d\u6587\u4ef6"
private const val LABEL_FOLDER = "\u6587\u4ef6\u5939"
private const val LABEL_VIDEO = "\u89c6\u9891"
private const val LABEL_FILE = "\u6587\u4ef6"
private const val LABEL_PAGE_CHOSEN = "\u5df2\u9009\u62e9\u7b2c%d\u9875"
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
    val context = LocalContext.current
    var activeToast by remember { mutableStateOf<Toast?>(null) }
    var lastPageButtonClickAt by remember { mutableLongStateOf(0L) }
    var pageButtonLocked by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
    )

    BackHandler(enabled = state.currentCid != "0") {
        viewModel.navigateUp()
    }

    LaunchedEffect(listState, state.files.size, state.hasMore, state.isLoadingMore) {
        listState.loadMoreWhenNeeded(
            itemCount = state.files.size,
            hasMore = state.hasMore,
            isLoadingMore = state.isLoadingMore,
            onLoadMore = viewModel::loadMore,
        )
    }

    LaunchedEffect(state.isLoading, state.isRefreshing, state.isLoadingMore) {
        if (!state.isLoading && !state.isRefreshing && !state.isLoadingMore) {
            pageButtonLocked = false
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
            NetDiskControls(
                state = state,
                onToggleVideoFilter = viewModel::toggleVideoFilter,
                onCycleSortOption = viewModel::cycleSortOption,
                onToggleOrder = viewModel::toggleSortOrder,
                onCyclePageSize = viewModel::cyclePageSize,
                onPreviousPage = { handlePagedAction(viewModel::previousPage) },
                onNextPage = { handlePagedAction(viewModel::nextPage) },
                pageButtonsEnabled = !pageButtonLocked,
            )

            NetDiskToolbar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                path = state.path,
                onOpenPath = viewModel::openPath,
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 0.dp,
                    bottom = contentPadding.calculateBottomPadding() + 10.dp,
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
                                onOpen = {
                                    if (file.isVideo && !file.pc.isNullOrBlank()) {
                                        context.startActivity(
                                            PlayerActivity.createNetDiskIntent(
                                                context = context,
                                                fileId = file.id,
                                                title = file.n,
                                                deleteLabel = file.n,
                                                pc = file.pc,
                                                isFavorite = file.isStarred,
                                            )
                                        )
                                    } else if (file.isDirectory) {
                                        viewModel.openDirectory(file)
                                    }
                                },
                                onOpenDetail = {
                                    context.startActivity(
                                        NetDiskDetailActivity.createIntent(
                                            context = context,
                                            id = file.id,
                                            title = file.n,
                                        )
                                    )
                                },
                            )
                        }

                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
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

@Composable
private fun NetDiskControls(
    state: NetDiskUiState,
    onToggleVideoFilter: () -> Unit,
    onCycleSortOption: () -> Unit,
    onToggleOrder: () -> Unit,
    onCyclePageSize: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    pageButtonsEnabled: Boolean,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NetDiskToolbarActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.VideoLibrary,
            contentDescription = "\u5168\u90e8/\u4ec5\u89c6\u9891",
            onClick = onToggleVideoFilter,
            containerColor = if (state.onlyVideos) colors.accentSoft else colors.surfaceVariant,
            contentColor = if (state.onlyVideos) colors.accentText else colors.textPrimary,
        )
        NetDiskToolbarActionButton(
            modifier = Modifier.weight(1f),
            icon = state.sortOption.icon(),
            contentDescription = "\u6392\u5e8f\u5b57\u6bb5",
            onClick = onCycleSortOption,
        )
        NetDiskToolbarActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.SwapVert,
            contentDescription = "\u6b63\u5012\u5e8f",
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
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "\u4e0a\u4e00\u9875",
            onClick = onPreviousPage,
            enabled = pageButtonsEnabled &&
                state.hasPreviousPage &&
                !state.isLoading &&
                !state.isRefreshing &&
                !state.isLoadingMore,
        )
        NetDiskToolbarActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = "\u4e0b\u4e00\u9875",
            onClick = onNextPage,
            enabled = pageButtonsEnabled &&
                state.hasNextPage &&
                !state.isLoading &&
                !state.isRefreshing &&
                !state.isLoadingMore,
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
            Icon(
                imageVector = file.typeIcon(),
                contentDescription = file.typeLabel(),
                tint = file.typeTint(),
                modifier = Modifier.size(28.dp),
            )

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
                Text(
                    text = file.uploadTime?.let(::formatListTime) ?: "-",
                    color = colors.textTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(
                onClick = onOpenDetail,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "\u8be6\u60c5",
                    tint = colors.textTertiary,
                )
            }
        }
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

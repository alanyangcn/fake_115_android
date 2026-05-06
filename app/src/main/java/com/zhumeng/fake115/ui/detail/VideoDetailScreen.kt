package com.zhumeng.fake115.ui.detail

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zhumeng.fake115.data.LibraryRepository
import com.zhumeng.fake115.data.model.VideoDetail
import com.zhumeng.fake115.ui.common.DeleteConfirmDialog
import com.zhumeng.fake115.ui.player.EmbeddedVideoPlayer
import com.zhumeng.fake115.ui.player.rememberManagedExoPlayer
import com.zhumeng.fake115.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
fun VideoDetailScreen(
    videoId: Int,
    repository: LibraryRepository,
    onBack: () -> Unit,
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<VideoDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var playRequested by rememberSaveable { mutableStateOf(false) }
    var resolvedUrl by rememberSaveable { mutableStateOf("") }
    var isResolving by rememberSaveable { mutableStateOf(false) }
    var isFavoriteUpdating by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var titleExpanded by rememberSaveable { mutableStateOf(false) }
    val exoPlayer = rememberManagedExoPlayer(
        url = resolvedUrl,
        requestHeaders = repository.build115RequestHeaders(),
    )

    LaunchedEffect(videoId) {
        if (videoId <= 0) {
            isLoading = false
            errorMessage = "影片不存在。"
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null
        runCatching { repository.fetchVideoDetail(videoId) }
            .onSuccess {
                detail = it
                isLoading = false
            }
            .onFailure { error ->
                errorMessage = error.message ?: "加载详情失败。"
                isLoading = false
            }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.appBackground,
    ) {
        if (isLandscape && playRequested) {
            when {
                isResolving -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                resolvedUrl.isNotBlank() -> {
                    exoPlayer?.let {
                        EmbeddedVideoPlayer(
                            exoPlayer = it,
                            modifier = Modifier.fillMaxSize(),
                            isFullScreen = true,
                            onToggleFullScreen = {},
                            showFullScreenButton = false,
                            forceFullScreen = true,
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("播放地址解析失败", color = colors.textPrimary)
                    }
                }
            }
            return@Surface
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            DetailTopBar(
                title = detail?.fanhao ?: "影片详情",
                onBack = onBack,
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                detail == null -> {
                    ErrorState(
                        message = errorMessage ?: "加载详情失败。",
                        onBack = onBack,
                    )
                }

                else -> {
                    val video = detail!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = video.name,
                            modifier = Modifier.clickable { titleExpanded = !titleExpanded },
                            color = colors.textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = if (titleExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        if (playRequested) {
                            when {
                                isResolving -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f / 9f),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }

                                resolvedUrl.isNotBlank() -> {
                                    exoPlayer?.let {
                                        EmbeddedVideoPlayer(
                                            exoPlayer = it,
                                            modifier = Modifier.fillMaxWidth(),
                                            isFullScreen = false,
                                            onToggleFullScreen = {},
                                            showFullScreenButton = false,
                                        )
                                    }
                                }

                                else -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f / 9f),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text("播放地址解析失败", color = colors.textPrimary)
                                        }
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
                            ) {
                                if (!video.cover.isNullOrBlank()) {
                                    AsyncImage(
                                        model = video.cover,
                                        contentDescription = video.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .background(colors.topBar),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("暂无封面", color = colors.textTertiary)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DetailActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.PlayArrow,
                                text = "播放",
                                onClick = {
                                    if (video.pc.isBlank()) {
                                        Toast.makeText(context, "缺少播放地址", Toast.LENGTH_SHORT).show()
                                        return@DetailActionButton
                                    }
                                    playRequested = true
                                    if (resolvedUrl.isNotBlank() || isResolving) return@DetailActionButton

                                    scope.launch {
                                        isResolving = true
                                        runCatching { repository.resolve115PlayableUrl(video.pc) }
                                            .onSuccess { url ->
                                                resolvedUrl = url
                                            }
                                            .onFailure { error ->
                                                Toast.makeText(
                                                    context,
                                                    error.message ?: "播放地址解析失败",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                        isResolving = false
                                    }
                                },
                            )
                            DetailActionButton(
                                modifier = Modifier.weight(1f),
                                icon = if (video.isFavorite == 1) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                text = "收藏",
                                enabled = !isFavoriteUpdating,
                                onClick = {
                                    if (isFavoriteUpdating) return@DetailActionButton
                                    scope.launch {
                                        isFavoriteUpdating = true
                                        val nextFavorite = video.isFavorite != 1
                                        runCatching {
                                            repository.updateFavorite(video.id, nextFavorite)
                                        }.onSuccess { favorite ->
                                            detail = video.copy(isFavorite = if (favorite) 1 else 0)
                                            Toast.makeText(
                                                context,
                                                if (favorite) "已收藏" else "已取消收藏",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }.onFailure { error ->
                                            Toast.makeText(
                                                context,
                                                error.message ?: "收藏操作失败",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                        isFavoriteUpdating = false
                                    }
                                },
                            )
                            DetailActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.Delete,
                                text = "删除",
                                enabled = !isDeleting,
                                containerColor = colors.dangerSoft,
                                onClick = { showDeleteDialog = true },
                            )
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                DetailFileNameCard(fileName = video.fileName)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    MetaStatCard(
                                        modifier = Modifier.weight(1f),
                                        title = "文件大小",
                                        value = formatFileSize(video.size),
                                    )
                                    MetaStatCard(
                                        modifier = Modifier.weight(1f),
                                        title = "发行日期",
                                        value = video.releaseDate ?: "-",
                                    )
                                    MetaStatCard(
                                        modifier = Modifier.weight(1f),
                                        title = "影片时长",
                                        value = formatDuration(video.duration),
                                    )
                                }

                                DetailTagSection(
                                    title = "制作商",
                                    tags = listOfNotNull(video.studio).ifEmpty { listOf("-") },
                                    containerColor = colors.accentSoft,
                                )
                                DetailTagSection(
                                    title = "发行商",
                                    tags = listOfNotNull(video.publisher).ifEmpty { listOf("-") },
                                    containerColor = colors.surfaceVariant,
                                )
                                DetailTagSection(
                                    title = "系列",
                                    tags = listOfNotNull(video.series).ifEmpty { listOf("-") },
                                    containerColor = colors.surfaceVariant,
                                )
                                DetailTagSection(
                                    title = "影片类别",
                                    tags = video.genres.ifEmpty { listOf("-") },
                                    containerColor = colors.surfaceVariant,
                                )
                                DetailTagSection(
                                    title = "演出人员",
                                    tags = video.actresses.ifEmpty { listOf("-") },
                                    containerColor = colors.dangerSoft,
                                )
                            }
                        }
                    }

                    if (showDeleteDialog) {
                        DeleteConfirmDialog(
                            message = "确定要删除 ${video.fanhao.ifBlank { video.name }} 吗？",
                            deleting = isDeleting,
                            onDismiss = {
                                if (!isDeleting) showDeleteDialog = false
                            },
                            onConfirm = {
                                scope.launch {
                                    isDeleting = true
                                    runCatching { repository.deleteMovie(video.id) }
                                        .onSuccess { message ->
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            showDeleteDialog = false
                                            onBack()
                                        }
                                        .onFailure { error ->
                                            Toast.makeText(
                                                context,
                                                error.message ?: "删除失败",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    isDeleting = false
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    title: String,
    onBack: () -> Unit,
) {
    val colors = AppTheme.colors
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        navigationIcon = {
            TextButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("返回")
            }
        },
        actions = {
            Spacer(modifier = Modifier.width(72.dp))
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = colors.topBar,
            titleContentColor = colors.textPrimary,
            navigationIconContentColor = colors.textPrimary,
            actionIconContentColor = colors.textPrimary,
        ),
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
    )
}

@Composable
private fun DetailActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    enabled: Boolean = true,
    containerColor: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val resolvedContainerColor = if (containerColor == Color.Unspecified) colors.surfaceVariant else containerColor
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = resolvedContainerColor,
            contentColor = colors.textPrimary,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text)
    }
}

@Composable
private fun DetailFileNameCard(fileName: String) {
    val colors = AppTheme.colors
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "文件名",
                color = colors.textTertiary,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = fileName,
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MetaStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
) {
    val colors = AppTheme.colors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                color = colors.textTertiary,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = value,
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailTagSection(
    title: String,
    tags: List<String>,
    containerColor: Color,
) {
    val colors = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = colors.textTertiary,
            style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tags.forEach { tag ->
                DetailTag(
                    text = tag,
                    containerColor = containerColor,
                )
            }
        }
    }
}

@Composable
private fun DetailTag(
    text: String,
    containerColor: Color,
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        tonalElevation = 0.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = colors.textPrimary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onBack: () -> Unit,
) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onBack) {
            Text("返回")
        }
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0L) return "-"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = size.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return "${DecimalFormat("0.##").format(value)} ${units[index]}"
}

private fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "-"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainSeconds = seconds % 60
    return buildString {
        if (hours > 0) append("${hours}小时")
        if (minutes > 0) append("${minutes}分钟")
        if (hours == 0 && remainSeconds > 0) append("${remainSeconds}秒")
    }
}

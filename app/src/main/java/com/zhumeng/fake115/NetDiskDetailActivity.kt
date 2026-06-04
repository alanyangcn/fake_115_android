package com.zhumeng.fake115

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.zhumeng.fake115.data.NetDiskRepository
import com.zhumeng.fake115.data.model.NetDiskDetailPathNode
import com.zhumeng.fake115.data.model.NetDiskFileDetail
import com.zhumeng.fake115.data.model.NetDiskFileLabel
import com.zhumeng.fake115.ui.theme.AppTheme
import com.zhumeng.fake115.ui.theme.Fake115Theme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DETAIL_TITLE_FALLBACK = "详情"
private const val DETAIL_BACK = "返回"
private const val DETAIL_LOAD_ERROR = "加载详情失败"

class NetDiskDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val id = intent.getStringExtra(EXTRA_ID).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val isDirectory = intent.getBooleanExtra(EXTRA_IS_DIRECTORY, false)

        setContent {
            Fake115Theme {
                NetDiskDetailScreen(
                    id = id,
                    title = title,
                    isDirectory = isDirectory,
                    onOpenPath = { cid ->
                        startActivity(MainActivity.createNetDiskIntent(this, cid))
                    },
                    onBack = ::finish,
                )
            }
        }
    }

    companion object {
        private const val EXTRA_ID = "extra_netdisk_id"
        private const val EXTRA_TITLE = "extra_netdisk_title"
        private const val EXTRA_IS_DIRECTORY = "extra_netdisk_is_directory"

        fun createIntent(
            context: Context,
            id: String,
            title: String,
            isDirectory: Boolean,
        ): Intent {
            return Intent(context, NetDiskDetailActivity::class.java).apply {
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_IS_DIRECTORY, isDirectory)
            }
        }
    }
}

@Composable
private fun NetDiskDetailScreen(
    id: String,
    title: String,
    isDirectory: Boolean,
    onOpenPath: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val repository = remember(context) { NetDiskRepository(context = context) }
    var detail by remember(id, isDirectory) { mutableStateOf<NetDiskFileDetail?>(null) }
    var errorMessage by remember(id, isDirectory) { mutableStateOf<String?>(null) }
    var isLoading by remember(id, isDirectory) { mutableStateOf(true) }

    fun reload() {
        isLoading = true
        errorMessage = null
    }

    LaunchedEffect(id, isDirectory, isLoading) {
        if (!isLoading) return@LaunchedEffect
        runCatching { repository.fetchFileDetail(id = id, isDirectory = isDirectory) }
            .onSuccess {
                detail = it
                isLoading = false
            }
            .onFailure { error ->
                errorMessage = error.message ?: DETAIL_LOAD_ERROR
                isLoading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground),
    ) {
        NetDiskDetailTopBar(
            title = detail?.let { if (it.isDirectory) "文件夹详情" else "文件详情" } ?: title,
            onBack = onBack,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> CircularProgressIndicator()
                errorMessage != null -> NetDiskDetailError(
                    message = errorMessage ?: DETAIL_LOAD_ERROR,
                    onRetry = ::reload,
                )
                detail != null -> NetDiskDetailContent(
                    detail = detail!!,
                    onOpenPath = onOpenPath,
                )
            }
        }
    }
}

@Composable
private fun NetDiskDetailTopBar(
    title: String,
    onBack: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = DETAIL_BACK,
                tint = colors.textPrimary,
            )
        }
        Text(
            text = title.ifBlank { DETAIL_TITLE_FALLBACK },
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NetDiskDetailContent(
    detail: NetDiskFileDetail,
    onOpenPath: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NetDiskDetailHero(detail = detail)

        DetailCard(title = "所在路径") {
            PathCrumbs(
                detail = detail,
                onOpenPath = onOpenPath,
            )
        }

        DetailCard(title = "时间") {
            TimeGrid(
                items = listOf(
                    "创建" to formatDetailTime(detail.createTime),
                    "上传" to formatDetailTime(detail.uploadTime),
                    "更新" to formatDetailTime(detail.updateTime),
                    "打开" to formatDetailTime(detail.openTime),
                )
            )
        }

        DetailCard(title = "评分") {
            RatingStars(score = detail.score.toFloatOrNull() ?: 0f)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusCard(
                label = "分享",
                value = if (detail.isShare) "是" else "否",
                active = detail.isShare,
                modifier = Modifier.weight(1f),
            )
            StatusCard(
                label = "私密",
                value = if (detail.isPrivate) "是" else "否",
                active = detail.isPrivate,
                modifier = Modifier.weight(1f),
            )
            StatusCard(
                label = "标记",
                value = if (detail.isMarked) "是" else "否",
                active = detail.isMarked,
                modifier = Modifier.weight(1f),
            )
        }

        DetailCard(title = "标签") {
            LabelList(labels = detail.labels)
        }

        DetailCard(title = "备注") {
            HtmlRemark(html = detail.desc)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NetDiskDetailHero(
    detail: NetDiskFileDetail,
) {
    val colors = AppTheme.colors
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailIconTile(
                icon = when {
                    detail.isDirectory -> Icons.Rounded.Folder
                    detail.playLongSeconds > 0L -> Icons.Rounded.Movie
                    else -> Icons.Rounded.InsertDriveFile
                },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = detail.fileName,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailChip(text = detail.size.ifBlank { "-" })
                    if (detail.isDirectory) {
                        DetailChip(text = "${detail.count.ifBlank { "0" }} 个文件")
                        DetailChip(text = "${detail.folderCount.ifBlank { "0" }} 个文件夹")
                    }
                    if (!detail.isDirectory && detail.playLongSeconds > 0L) {
                        DetailChip(text = formatDuration(detail.playLongSeconds))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailIconTile(
    icon: ImageVector,
) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .size(54.dp)
            .background(colors.accentSoft, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun DetailCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            content()
        }
    }
}

@Composable
private fun PathCrumbs(
    detail: NetDiskFileDetail,
    onOpenPath: (String) -> Unit,
) {
    val colors = AppTheme.colors
    val paths = detail.paths.ifEmpty {
        listOf(NetDiskDetailPathNode(fileId = "0", fileName = "根目录"))
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        paths.forEachIndexed { index, path ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = path.fileName,
                    color = colors.accentText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(colors.accentSoft, RoundedCornerShape(8.dp))
                        .clickable { onOpenPath(path.fileId.ifBlank { "0" }) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                )
                if (index < paths.lastIndex) {
                    Text(
                        text = ">",
                        color = colors.textTertiary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeGrid(
    items: List<Pair<String, String>>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { (label, value) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = label,
                            color = AppTheme.colors.textTertiary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = value,
                            color = AppTheme.colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (rowItems.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DetailChip(
    text: String,
    onClick: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val chipModifier = Modifier
        .background(colors.accentSoft, RoundedCornerShape(999.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 10.dp, vertical = 6.dp)
    Box(
        modifier = chipModifier,
    ) {
        Text(
            text = text,
            color = colors.accentText,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelList(
    labels: List<NetDiskFileLabel>,
) {
    if (labels.isEmpty()) {
        Text(
            text = "无标签",
            color = AppTheme.colors.textTertiary,
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEach { label ->
            LabelChip(label = label)
        }
    }
}

@Composable
private fun LabelChip(
    label: NetDiskFileLabel,
) {
    val fallbackColor = AppTheme.colors.accent
    val labelColor = parseHexColor(label.color) ?: fallbackColor
    Box(
        modifier = Modifier
            .background(labelColor.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label.name,
            color = labelColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HtmlRemark(
    html: String,
) {
    val colors = AppTheme.colors
    if (html.isBlank()) {
        Text(
            text = "无备注",
            color = colors.textTertiary,
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            TextView(context).apply {
                setTextColor(colors.textSecondary.toArgb())
                textSize = 14f
                includeFontPadding = false
            }
        },
        update = { textView ->
            textView.setTextColor(colors.textSecondary.toArgb())
            textView.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(html)
            }
        },
    )
}

@Composable
private fun RatingStars(
    score: Float,
) {
    val colors = AppTheme.colors
    val activeStars = score.coerceIn(0f, 5f).toInt()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(5) { index ->
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = if (index < activeStars) Color(0xFFFFC857) else colors.textTertiary.copy(alpha = 0.28f),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Text(
            text = "${score.coerceIn(0f, 5f).toInt()}/5",
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun StatusCard(
    label: String,
    value: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) colors.accentSoft else colors.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                color = colors.textTertiary,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = value,
                color = if (active) colors.accentText else colors.textSecondary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun NetDiskDetailError(
    message: String,
    onRetry: () -> Unit,
) {
    val colors = AppTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

private fun formatDetailTime(timestamp: Long?): String {
    val seconds = timestamp ?: return "-"
    val millis = seconds * 1000L
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))
    }.getOrDefault(seconds.toString())
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0L) return "-"
    val hours = seconds / 3600L
    val minutes = (seconds % 3600L) / 60L
    val remainingSeconds = seconds % 60L
    return if (hours > 0L) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, remainingSeconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }
}

private fun parseHexColor(value: String): Color? {
    val normalized = value.trim()
    if (!normalized.startsWith("#")) return null
    return runCatching {
        Color(android.graphics.Color.parseColor(normalized))
    }.getOrNull()
}

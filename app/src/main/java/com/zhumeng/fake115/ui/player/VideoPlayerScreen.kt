package com.zhumeng.fake115.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.BatteryManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenRotationAlt
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.zhumeng.fake115.data.AppSettings
import com.zhumeng.fake115.ui.common.DeleteConfirmDialog
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PlayerPlaylistItem(
    val itemId: String,
    val title: String,
    val deleteLabel: String,
    val pc: String,
    val isFavorite: Boolean,
)

@Composable
fun VideoPlayerScreen(
    itemId: String,
    title: String,
    deleteLabel: String = "",
    initialUrl: String,
    pc: String = "",
    initialFavorite: Boolean = false,
    requestHeaders: Map<String, String> = emptyMap(),
    resolveUrl: (suspend (String) -> String)? = null,
    updateFavorite: (suspend (String, Boolean) -> Boolean)? = null,
    deleteVideo: (suspend (String) -> String)? = null,
    classifyVideo: (suspend (String) -> String)? = null,
    autoPlayNextAfterFavorite: Boolean = false,
    playlist: List<PlayerPlaylistItem> = emptyList(),
    currentPlaylistIndex: Int = -1,
    onPlaylistItemSelected: (Int) -> Unit = {},
    onPlaybackEnded: () -> Unit = {},
    onBack: () -> Unit,
    onDeleteCompleted: () -> Unit = onBack,
) {
    val context = LocalContext.current
    var resolvedUrl by rememberSaveable(itemId, pc) { mutableStateOf(initialUrl) }
    var isResolving by rememberSaveable(itemId, pc) { mutableStateOf(initialUrl.isBlank() && pc.isNotBlank()) }
    var errorMessage by rememberSaveable(itemId, pc) { mutableStateOf<String?>(null) }
    var isFavorite by rememberSaveable(itemId) { mutableStateOf(initialFavorite) }
    var isFavoriteUpdating by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var isClassifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val exoPlayer = rememberManagedExoPlayer(
        url = resolvedUrl,
        requestHeaders = requestHeaders,
    )
    val activity = context.findActivity()

    LaunchedEffect(itemId) {
        isDeleting = false
        isClassifying = false
        isFavoriteUpdating = false
        errorMessage = null
    }

    DisposableEffect(activity) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        }
        onDispose {
            if (window != null) {
                WindowInsetsControllerCompat(window, window.decorView)
                    .show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    LaunchedEffect(itemId, initialUrl, pc) {
        if (initialUrl.isNotBlank()) {
            resolvedUrl = initialUrl
            isResolving = false
            errorMessage = null
            return@LaunchedEffect
        }
        if (pc.isBlank() || resolveUrl == null) return@LaunchedEffect

        isResolving = true
        errorMessage = null
        runCatching { resolveUrl(pc) }
            .onSuccess { url ->
                resolvedUrl = url
                isResolving = false
            }
            .onFailure { error ->
                resolvedUrl = ""
                isResolving = false
                errorMessage = error.message ?: "Failed to resolve playback url."
            }
    }

    val handleToggleFavorite: () -> Unit = {
        if (updateFavorite != null && itemId.isNotBlank() && !isFavoriteUpdating) {
            scope.launch {
                isFavoriteUpdating = true
                runCatching { updateFavorite(itemId, !isFavorite) }
                    .onSuccess { favorite ->
                        isFavorite = favorite
                        if (
                            autoPlayNextAfterFavorite &&
                            currentPlaylistIndex in playlist.indices &&
                            currentPlaylistIndex < playlist.lastIndex
                        ) {
                            onPlaylistItemSelected(currentPlaylistIndex + 1)
                        }
                    }
                    .onFailure { error ->
                        errorMessage = error.message ?: "Failed to update favorite."
                    }
                isFavoriteUpdating = false
            }
        }
    }

    fun favoritePlaylistItemAndPlayNext(index: Int) {
        val item = playlist.getOrNull(index) ?: return
        if (updateFavorite == null || item.itemId.isBlank()) return
        scope.launch {
            runCatching { updateFavorite(item.itemId, !item.isFavorite) }
                .onSuccess { favorite ->
                    if (item.itemId == itemId) {
                        isFavorite = favorite
                    }
                    if (autoPlayNextAfterFavorite && index < playlist.lastIndex) {
                        onPlaylistItemSelected(index + 1)
                    }
                }
                .onFailure { error ->
                    errorMessage = error.message ?: "Failed to update favorite."
                }
        }
    }

    val handleDelete: () -> Unit = {
        if (deleteVideo != null && itemId.isNotBlank() && !isDeleting) {
            scope.launch {
                isDeleting = true
                runCatching { deleteVideo(itemId) }
                    .onSuccess {
                        isDeleting = false
                        onDeleteCompleted()
                    }
                    .onFailure { error ->
                        errorMessage = error.message ?: "Failed to delete video."
                        isDeleting = false
                    }
            }
        }
    }

    val handleClassify: () -> Unit = {
        if (classifyVideo != null && itemId.isNotBlank() && !isClassifying) {
            scope.launch {
                isClassifying = true
                runCatching { classifyVideo(itemId) }
                    .onSuccess {
                        isClassifying = false
                        onDeleteCompleted()
                    }
                    .onFailure { error ->
                        errorMessage = error.message ?: "Failed to classify video."
                        isClassifying = false
                    }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                exoPlayer != null -> {
                    EmbeddedVideoPlayer(
                        exoPlayer = exoPlayer,
                        modifier = Modifier.fillMaxSize(),
                        isFullScreen = true,
                        onToggleFullScreen = {},
                        isFavorite = isFavorite,
                        favoriteEnabled = !isFavoriteUpdating && updateFavorite != null && itemId.isNotBlank(),
                        deleteEnabled = !isDeleting && deleteVideo != null && itemId.isNotBlank(),
                        classifyEnabled = !isClassifying && classifyVideo != null && itemId.isNotBlank(),
                        deleteInProgress = isDeleting,
                        onToggleFavorite = handleToggleFavorite,
                        onDelete = handleDelete,
                        onClassify = handleClassify,
                        playlist = playlist,
                        currentPlaylistIndex = currentPlaylistIndex,
                        onPlaylistItemSelected = onPlaylistItemSelected,
                        onPlaylistFavoriteSelected = ::favoritePlaylistItemAndPlayNext,
                        onPlaybackEnded = onPlaybackEnded,
                        deleteLabel = deleteLabel,
                        title = title.ifBlank { "Video" },
                        onBack = onBack,
                        progressKey = itemId.ifBlank { pc.ifBlank { resolvedUrl } },
                        showTopChrome = true,
                        showFullScreenButton = false,
                        forceFullScreen = true,
                    )
                }

                else -> {
                    PlayerUnavailableShell(
                        title = title.ifBlank { "Video" },
                        isFavorite = isFavorite,
                        favoriteEnabled = !isFavoriteUpdating && updateFavorite != null && itemId.isNotBlank(),
                        deleteEnabled = !isDeleting && deleteVideo != null && itemId.isNotBlank(),
                        classifyEnabled = !isClassifying && classifyVideo != null && itemId.isNotBlank(),
                        deleteInProgress = isDeleting,
                        onToggleFavorite = handleToggleFavorite,
                        onDelete = handleDelete,
                        onClassify = handleClassify,
                        playlist = playlist,
                        currentPlaylistIndex = currentPlaylistIndex,
                        onPlaylistItemSelected = onPlaylistItemSelected,
                        onPlaylistFavoriteSelected = ::favoritePlaylistItemAndPlayNext,
                        deleteLabel = deleteLabel,
                        onBack = onBack,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (isResolving) {
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage?.let { message ->
                PlaybackErrorMessage(
                    message = message,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun PlayerUnavailableShell(
    title: String,
    isFavorite: Boolean,
    favoriteEnabled: Boolean,
    deleteEnabled: Boolean,
    classifyEnabled: Boolean,
    deleteInProgress: Boolean,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onClassify: () -> Unit,
    playlist: List<PlayerPlaylistItem>,
    currentPlaylistIndex: Int,
    onPlaylistItemSelected: (Int) -> Unit,
    onPlaylistFavoriteSelected: (Int) -> Unit,
    deleteLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val quickManagementEnabled by AppSettings.quickManagementEnabledFlow(context).collectAsState()
    val activity = context.findActivity()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var resizeMode by rememberSaveable { mutableStateOf(VideoResizeMode.Fit) }
    var playbackSpeed by rememberSaveable { mutableFloatStateOf(1f) }
    var batteryLevel by remember { mutableStateOf(readBatteryLevel(context)) }
    var currentTime by remember { mutableStateOf(currentTimeText()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = currentTimeText()
            batteryLevel = readBatteryLevel(context)
            delay(1000)
        }
    }

    Box(
        modifier = modifier.background(Color.Black)
    ) {
        PlayerChromeScrims(modifier = Modifier.fillMaxSize())

        PlayerTopOverlay(
            title = title,
            currentTime = currentTime,
            batteryLevel = batteryLevel,
            resizeMode = resizeMode,
            isFavorite = isFavorite,
            favoriteEnabled = favoriteEnabled,
            deleteEnabled = deleteEnabled,
            classifyEnabled = classifyEnabled,
            showPlaylistButton = playlist.isNotEmpty(),
            onBack = onBack,
            onOpenPlaylist = { showPlaylistDialog = true },
            onCycleResizeMode = { resizeMode = resizeMode.next() },
            onToggleFavorite = onToggleFavorite,
            onClassify = onClassify,
            onDelete = {
                if (quickManagementEnabled) {
                    onDelete()
                } else {
                    showDeleteDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )

        VideoControlsOverlay(
            isPlaying = false,
            positionMs = 0L,
            durationMs = 0L,
            onPlayPause = {},
            canPlayPrevious = currentPlaylistIndex > 0 && playlist.isNotEmpty(),
            onPlayPrevious = { onPlaylistItemSelected(currentPlaylistIndex - 1) },
            canPlayNext = currentPlaylistIndex >= 0 &&
                currentPlaylistIndex < playlist.lastIndex &&
                playlist.isNotEmpty(),
            onPlayNext = { onPlaylistItemSelected(currentPlaylistIndex + 1) },
            onSeekChange = {},
            onSeekEnd = {},
            playbackSpeed = playbackSpeed,
            onPlaybackSpeedSelected = { playbackSpeed = it },
            onToggleOrientation = {
                activity?.requestedOrientation =
                    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                    }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }

    if (showPlaylistDialog) {
        PlaylistDialog(
            playlist = playlist,
            currentIndex = currentPlaylistIndex,
            onDismiss = { showPlaylistDialog = false },
            onItemSelected = { index ->
                showPlaylistDialog = false
                onPlaylistItemSelected(index)
            },
            onFavoriteSelected = { index ->
                showPlaylistDialog = false
                onPlaylistFavoriteSelected(index)
            },
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            message = "确定要删除《${title.ifBlank { deleteLabel.ifBlank { "当前影片" } }}》吗？",
            deleting = deleteInProgress,
            onDismiss = {
                if (!deleteInProgress) showDeleteDialog = false
            },
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            confirmEnabled = deleteEnabled,
        )
    }
}

@Composable
private fun PlaybackErrorMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(horizontal = 24.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xB3000000),
    ) {
        Text(
            text = message.ifBlank { "播放地址解析失败" },
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}

@Composable
fun EmbeddedVideoPlayer(
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    isFavorite: Boolean = false,
    favoriteEnabled: Boolean = false,
    deleteEnabled: Boolean = false,
    classifyEnabled: Boolean = false,
    deleteInProgress: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onDelete: () -> Unit = {},
    onClassify: () -> Unit = {},
    playlist: List<PlayerPlaylistItem> = emptyList(),
    currentPlaylistIndex: Int = -1,
    onPlaylistItemSelected: (Int) -> Unit = {},
    onPlaylistFavoriteSelected: (Int) -> Unit = {},
    onPlaybackEnded: () -> Unit = {},
    title: String = "",
    deleteLabel: String = "",
    onBack: () -> Unit = {},
    progressKey: String = "",
    showTopChrome: Boolean = false,
    showFullScreenButton: Boolean = true,
    forceFullScreen: Boolean = false,
) {
    if (isFullScreen || forceFullScreen) {
        if (!forceFullScreen) {
            BackHandler(onBack = onToggleFullScreen)
        }
        if (forceFullScreen) {
            Surface(modifier = modifier.fillMaxSize(), color = Color.Black) {
                PlayerContainer(
                    exoPlayer = exoPlayer,
                    isFavorite = isFavorite,
                    favoriteEnabled = favoriteEnabled,
                    deleteEnabled = deleteEnabled,
                    classifyEnabled = classifyEnabled,
                    deleteInProgress = deleteInProgress,
                    onToggleFavorite = onToggleFavorite,
                    onDelete = onDelete,
                    onClassify = onClassify,
                    playlist = playlist,
                    currentPlaylistIndex = currentPlaylistIndex,
                    onPlaylistItemSelected = onPlaylistItemSelected,
                    onPlaylistFavoriteSelected = onPlaylistFavoriteSelected,
                    onPlaybackEnded = onPlaybackEnded,
                    title = title,
                    deleteLabel = deleteLabel,
                    onBack = onBack,
                    progressKey = progressKey,
                    showTopChrome = showTopChrome,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            Dialog(
                onDismissRequest = onToggleFullScreen,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        PlayerContainer(
                            exoPlayer = exoPlayer,
                            isFavorite = isFavorite,
                            favoriteEnabled = favoriteEnabled,
                            deleteEnabled = deleteEnabled,
                            classifyEnabled = classifyEnabled,
                            deleteInProgress = deleteInProgress,
                            onToggleFavorite = onToggleFavorite,
                            onDelete = onDelete,
                            onClassify = onClassify,
                            playlist = playlist,
                            currentPlaylistIndex = currentPlaylistIndex,
                            onPlaylistItemSelected = onPlaylistItemSelected,
                            onPlaylistFavoriteSelected = onPlaylistFavoriteSelected,
                            onPlaybackEnded = onPlaybackEnded,
                            title = title,
                            deleteLabel = deleteLabel,
                            onBack = onBack,
                            progressKey = progressKey,
                            showTopChrome = showTopChrome,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                PlayerContainer(
                    exoPlayer = exoPlayer,
                    isFavorite = isFavorite,
                    favoriteEnabled = favoriteEnabled,
                    deleteEnabled = deleteEnabled,
                    classifyEnabled = classifyEnabled,
                    deleteInProgress = deleteInProgress,
                    onToggleFavorite = onToggleFavorite,
                    onDelete = onDelete,
                    onClassify = onClassify,
                    playlist = playlist,
                    currentPlaylistIndex = currentPlaylistIndex,
                    onPlaylistItemSelected = onPlaylistItemSelected,
                    onPlaylistFavoriteSelected = onPlaylistFavoriteSelected,
                    onPlaybackEnded = onPlaybackEnded,
                    title = title,
                    deleteLabel = deleteLabel,
                    onBack = onBack,
                    progressKey = progressKey,
                    showTopChrome = showTopChrome,
                    modifier = Modifier.matchParentSize()
                )

                if (showFullScreenButton) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = onToggleFullScreen) {
                            Text("Full Screen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberManagedExoPlayer(
    url: String,
    requestHeaders: Map<String, String>,
): ExoPlayer? {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = remember(context, url, requestHeaders) {
        if (url.isBlank()) {
            null
        } else {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(requestHeaders)
            val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

            ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .build()
                .apply {
                    repeatMode = Player.REPEAT_MODE_OFF
                    playWhenReady = true
                    setMediaItem(MediaItem.fromUri(url))
                    prepare()
                }
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        if (exoPlayer == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> exoPlayer.play()
                    Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                    else -> Unit
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                exoPlayer.release()
            }
        }
    }

    return exoPlayer
}

@Composable
private fun PlayerContainer(
    exoPlayer: ExoPlayer,
    isFavorite: Boolean,
    favoriteEnabled: Boolean,
    deleteEnabled: Boolean,
    classifyEnabled: Boolean,
    deleteInProgress: Boolean,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onClassify: () -> Unit,
    playlist: List<PlayerPlaylistItem>,
    currentPlaylistIndex: Int,
    onPlaylistItemSelected: (Int) -> Unit,
    onPlaylistFavoriteSelected: (Int) -> Unit,
    onPlaybackEnded: () -> Unit,
    title: String,
    deleteLabel: String,
    onBack: () -> Unit,
    progressKey: String,
    showTopChrome: Boolean,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val quickManagementEnabled by AppSettings.quickManagementEnabledFlow(context).collectAsState()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val activity = context.findActivity()
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    var positionMs by remember(exoPlayer) { mutableLongStateOf(0L) }
    var durationMs by remember(exoPlayer) { mutableLongStateOf(0L) }
    var isPlaying by remember(exoPlayer) { mutableStateOf(exoPlayer.isPlaying) }
    var isSeeking by remember { mutableStateOf(false) }
    var sliderPositionMs by remember { mutableFloatStateOf(0f) }
    var playbackSpeed by remember(exoPlayer) { mutableStateOf(exoPlayer.playbackParameters.speed) }
    var resizeMode by rememberSaveable { mutableStateOf(VideoResizeMode.Fit) }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var controlsAutoHideNonce by remember { mutableLongStateOf(0L) }
    var isControlsLocked by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var isFastModeActive by remember { mutableStateOf(false) }
    var isRetryingPlayback by remember { mutableStateOf(false) }
    var retryAttempt by remember(exoPlayer) { mutableStateOf(0) }
    var previousControlsVisible by remember { mutableStateOf(true) }
    var skipNextTap by remember { mutableStateOf(false) }
    var showSeekPreview by remember { mutableStateOf(false) }
    var seekPreviewTargetMs by remember { mutableLongStateOf(0L) }
    var seekGestureStartPositionMs by remember { mutableLongStateOf(0L) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var brightnessIndicatorJob by remember { mutableStateOf<Job?>(null) }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var volumeIndicatorJob by remember { mutableStateOf<Job?>(null) }
    var brightnessLevel by remember {
        mutableStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f)
    }
    var volumeLevel by remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }
    var batteryLevel by remember { mutableStateOf(readBatteryLevel(context)) }
    var currentTime by remember { mutableStateOf(currentTimeText()) }
    val progressPrefs = remember(context) {
        context.getSharedPreferences(PLAYER_PROGRESS_PREFS, Context.MODE_PRIVATE)
    }
    val progressPrefKey = remember(progressKey) { "position:$progressKey" }
    val markControlsInteraction: () -> Unit = {
        controlsAutoHideNonce += 1L
    }
    val savePlaybackProgress: () -> Unit = {
        val duration = exoPlayer.duration
        val position = exoPlayer.currentPosition.coerceAtLeast(0L)
        if (progressKey.isNotBlank() && duration > RESUME_MIN_DURATION_MS) {
            if (position < RESUME_MIN_POSITION_MS || duration - position < RESUME_END_CLEAR_WINDOW_MS) {
                progressPrefs.edit().remove(progressPrefKey).apply()
            } else {
                progressPrefs.edit().putLong(progressPrefKey, position).apply()
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = currentTimeText()
            batteryLevel = readBatteryLevel(context)
            delay(1000)
        }
    }

    LaunchedEffect(exoPlayer, progressKey) {
        val savedPosition = progressPrefs.getLong(progressPrefKey, 0L)
        if (savedPosition >= RESUME_MIN_POSITION_MS) {
            exoPlayer.seekTo(savedPosition)
            positionMs = savedPosition
        } else if (progressKey.isNotBlank()) {
            exoPlayer.seekTo(0L)
            positionMs = 0L
        }
    }

    DisposableEffect(exoPlayer, progressKey) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
                if (!isSeeking) {
                    positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                }
                if (playbackState == Player.STATE_READY) {
                    isRetryingPlayback = false
                    retryAttempt = 0
                } else if (playbackState == Player.STATE_ENDED) {
                    progressPrefs.edit().remove(progressPrefKey).apply()
                    onPlaybackEnded()
                }
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                playbackSpeed = playbackParameters.speed
            }

            override fun onPlayerError(error: PlaybackException) {
                if (retryAttempt >= PLAYBACK_RETRY_LIMIT) {
                    isRetryingPlayback = false
                    return
                }

                retryAttempt += 1
                isRetryingPlayback = true
                scope.launch {
                    delay(PLAYBACK_RETRY_DELAY_MS * retryAttempt)
                    exoPlayer.prepare()
                    exoPlayer.play()
                }
            }
        }

        exoPlayer.addListener(listener)
        durationMs = exoPlayer.duration.coerceAtLeast(0L)
        positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
        isPlaying = exoPlayer.isPlaying
        playbackSpeed = exoPlayer.playbackParameters.speed
        onDispose {
            savePlaybackProgress()
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(exoPlayer, progressKey) {
        while (true) {
            delay(PROGRESS_SAVE_INTERVAL_MS)
            savePlaybackProgress()
        }
    }

    LaunchedEffect(exoPlayer, isSeeking) {
        while (true) {
            if (!isSeeking) {
                positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
                isPlaying = exoPlayer.isPlaying
                playbackSpeed = exoPlayer.playbackParameters.speed
            }
            delay(500)
        }
    }

    DisposableEffect(activity, isPlaying) {
        val window = activity?.window
        if (window != null && isPlaying) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(
        controlsVisible,
        controlsAutoHideNonce,
        isFastModeActive,
        isSeeking,
        showDeleteDialog,
        showSeekPreview,
    ) {
        if (
            controlsVisible &&
            !isFastModeActive &&
            !isSeeking &&
            !showDeleteDialog &&
            !showSeekPreview
        ) {
            delay(5_000L)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(exoPlayer, containerSize, isControlsLocked) {
                detectTapGestures(
                    onPress = {
                        if (isControlsLocked || containerSize.width <= 0) {
                            return@detectTapGestures
                        }

                        val speedBeforeFastMode = playbackSpeed
                        val controlsBeforeFastMode = controlsVisible
                        var enteredFastMode = false

                        coroutineScope {
                            val activationJob = launch {
                                delay(280L)
                                enteredFastMode = true
                                skipNextTap = true
                                previousControlsVisible = controlsBeforeFastMode
                                controlsVisible = false
                                markControlsInteraction()
                                isFastModeActive = true
                                exoPlayer.setPlaybackParameters(PlaybackParameters(3f))
                                playbackSpeed = 3f
                            }

                            tryAwaitRelease()
                            activationJob.cancel()
                        }

                        if (enteredFastMode) {
                            exoPlayer.setPlaybackParameters(PlaybackParameters(speedBeforeFastMode))
                            playbackSpeed = speedBeforeFastMode
                            isFastModeActive = false
                            controlsVisible = previousControlsVisible
                            markControlsInteraction()
                        }
                    },
                    onTap = { _ ->
                        if (isControlsLocked) {
                            controlsVisible = true
                            markControlsInteraction()
                            return@detectTapGestures
                        }
                        if (skipNextTap) {
                            skipNextTap = false
                            return@detectTapGestures
                        }
                        controlsVisible = !controlsVisible
                        markControlsInteraction()
                    },
                    onDoubleTap = { _ ->
                        if (isControlsLocked) return@detectTapGestures
                        markControlsInteraction()
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    }
                )
            }
            .pointerInput(audioManager, exoPlayer, isControlsLocked) {
                var initialTouchX = 0f
                var accumulatedX = 0f
                var accumulatedY = 0f
                var lockedTarget: PlayerGestureTarget? = null

                detectDragGestures(
                    onDragStart = { offset ->
                        if (!isControlsLocked) {
                            initialTouchX = offset.x
                            accumulatedX = 0f
                            accumulatedY = 0f
                            lockedTarget = null
                            seekGestureStartPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                            markControlsInteraction()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (isControlsLocked) return@detectDragGestures
                        if (size.height <= 0 || size.width <= 0) return@detectDragGestures

                        accumulatedX += dragAmount.x
                        accumulatedY += dragAmount.y

                        if (lockedTarget == null) {
                            val absX = kotlin.math.abs(accumulatedX)
                            val absY = kotlin.math.abs(accumulatedY)

                            if (maxOf(absX, absY) < 12f) {
                                return@detectDragGestures
                            }

                            if (absX > absY) {
                                lockedTarget = PlayerGestureTarget.Seek
                                controlsVisible = false
                            } else {
                                lockedTarget = if (initialTouchX <= size.width / 2f) {
                                    PlayerGestureTarget.Brightness
                                } else {
                                    PlayerGestureTarget.Volume
                                }
                            }
                        }

                        change.consume()
                        val dragRatio = (-dragAmount.y / size.height.toFloat()).coerceIn(-1f, 1f)
                        when (lockedTarget) {
                            PlayerGestureTarget.Seek -> {
                                val safeDuration = exoPlayer.duration.takeIf { it > 0 } ?: return@detectDragGestures
                                val maxSeekMs = minOf(safeDuration / 2L, 10 * 60 * 1000L).coerceAtLeast(30_000L)
                                val deltaMs = (accumulatedX / size.width.toFloat() * maxSeekMs).toLong()
                                seekPreviewTargetMs = (seekGestureStartPositionMs + deltaMs)
                                    .coerceIn(0L, safeDuration)
                                showSeekPreview = true
                            }

                            PlayerGestureTarget.Brightness -> {
                                brightnessLevel = (brightnessLevel + dragRatio).coerceIn(0.05f, 1f)
                                activity?.window?.attributes = activity?.window?.attributes?.apply {
                                    screenBrightness = brightnessLevel
                                }
                                showBrightnessIndicator = true
                                brightnessIndicatorJob?.cancel()
                                brightnessIndicatorJob = scope.launch {
                                    delay(900L)
                                    showBrightnessIndicator = false
                                }
                            }

                            PlayerGestureTarget.Volume -> {
                                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                val nextVolume = (
                                    volumeLevel + dragRatio * maxVolume * 2f
                                ).coerceIn(0f, maxVolume.toFloat())
                                volumeLevel = nextVolume
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    nextVolume.toInt(),
                                    0
                                )
                                showVolumeIndicator = true
                                volumeIndicatorJob?.cancel()
                                volumeIndicatorJob = scope.launch {
                                    delay(900L)
                                    showVolumeIndicator = false
                                }
                            }

                            null -> Unit
                        }
                    },
                    onDragEnd = {
                        if (lockedTarget == PlayerGestureTarget.Seek && showSeekPreview) {
                            exoPlayer.seekTo(seekPreviewTargetMs)
                            positionMs = seekPreviewTargetMs
                            showSeekPreview = false
                            controlsVisible = true
                            markControlsInteraction()
                        }
                        initialTouchX = 0f
                        accumulatedX = 0f
                        accumulatedY = 0f
                        lockedTarget = null
                    },
                    onDragCancel = {
                        showSeekPreview = false
                        initialTouchX = 0f
                        accumulatedX = 0f
                        accumulatedY = 0f
                        lockedTarget = null
                    }
                )
            }
    ) {
        PlayerSurface(
            exoPlayer = exoPlayer,
            resizeMode = resizeMode.playerViewMode,
            modifier = Modifier.matchParentSize()
        )

        AnimatedVisibility(
            visible = controlsVisible && !isFastModeActive && !isControlsLocked,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            PlayerChromeScrims(modifier = Modifier.fillMaxSize())
        }

        AnimatedVisibility(
            visible = showTopChrome && controlsVisible && !isFastModeActive && !isControlsLocked,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            PlayerTopOverlay(
                title = title,
                currentTime = currentTime,
                batteryLevel = batteryLevel,
                resizeMode = resizeMode,
                isFavorite = isFavorite,
                favoriteEnabled = favoriteEnabled,
                deleteEnabled = deleteEnabled,
                classifyEnabled = classifyEnabled,
                showPlaylistButton = playlist.isNotEmpty(),
                onBack = onBack,
                onOpenPlaylist = {
                    markControlsInteraction()
                    showPlaylistDialog = true
                },
                onCycleResizeMode = {
                    markControlsInteraction()
                    resizeMode = resizeMode.next()
                },
                onToggleFavorite = {
                    markControlsInteraction()
                    onToggleFavorite()
                },
                onClassify = {
                    markControlsInteraction()
                    onClassify()
                },
                onDelete = {
                    markControlsInteraction()
                    if (quickManagementEnabled) {
                        onDelete()
                    } else {
                        showDeleteDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }

        AnimatedVisibility(
            visible = controlsVisible && !isFastModeActive && !isControlsLocked,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            VideoControlsOverlay(
                isPlaying = isPlaying,
                positionMs = if (isSeeking) sliderPositionMs.toLong() else positionMs,
                durationMs = durationMs,
                onPlayPause = {
                    markControlsInteraction()
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                },
                canPlayPrevious = currentPlaylistIndex > 0 && playlist.isNotEmpty(),
                onPlayPrevious = {
                    markControlsInteraction()
                    onPlaylistItemSelected(currentPlaylistIndex - 1)
                },
                canPlayNext = currentPlaylistIndex >= 0 &&
                    currentPlaylistIndex < playlist.lastIndex &&
                    playlist.isNotEmpty(),
                onPlayNext = {
                    markControlsInteraction()
                    onPlaylistItemSelected(currentPlaylistIndex + 1)
                },
                onSeekChange = { value ->
                    markControlsInteraction()
                    if (!isSeeking) {
                        isSeeking = true
                    }
                    sliderPositionMs = value
                },
                onSeekEnd = {
                    exoPlayer.seekTo(sliderPositionMs.toLong())
                    positionMs = sliderPositionMs.toLong()
                    isSeeking = false
                    markControlsInteraction()
                },
                playbackSpeed = playbackSpeed,
                onPlaybackSpeedSelected = { nextSpeed ->
                    markControlsInteraction()
                    exoPlayer.setPlaybackParameters(PlaybackParameters(nextSpeed))
                    playbackSpeed = nextSpeed
                },
                onToggleOrientation = {
                    markControlsInteraction()
                    activity?.requestedOrientation =
                        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        AnimatedVisibility(
            visible = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                !isFastModeActive &&
                (controlsVisible || isControlsLocked),
            modifier = Modifier.align(Alignment.CenterStart),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            PlayerLockButton(
                locked = isControlsLocked,
                onClick = {
                    isControlsLocked = !isControlsLocked
                    controlsVisible = true
                    markControlsInteraction()
                },
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 14.dp)
            )
        }

        if (isRetryingPlayback) {
            PlaybackRetryIndicator(
                retryAttempt = retryAttempt,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (showSeekPreview) {
            SeekPreviewIndicator(
                targetPositionMs = seekPreviewTargetMs,
                durationMs = durationMs,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 84.dp)
            )
        }

        if (isFastModeActive) {
            FastModeIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = with(density) { 56.dp })
            )
        }

        if (showBrightnessIndicator) {
            SideLevelIndicator(
                icon = Icons.Rounded.LightMode,
                levelFraction = brightnessLevel.coerceIn(0f, 1f),
                levelPercent = (brightnessLevel * 100).toInt().coerceIn(0, 100),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(
                        start = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            68.dp
                        } else {
                            12.dp
                        }
                    )
            )
        }

        if (showVolumeIndicator) {
            SideLevelIndicator(
                icon = Icons.Rounded.VolumeUp,
                levelFraction = (
                    volumeLevel /
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1).toFloat()
                    ).coerceIn(0f, 1f),
                levelPercent = (
                    volumeLevel * 100 /
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1).toFloat()
                    ).toInt().coerceIn(0, 100),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(end = 16.dp)
            )
        }
    }

    if (showPlaylistDialog) {
        PlaylistDialog(
            playlist = playlist,
            currentIndex = currentPlaylistIndex,
            onDismiss = { showPlaylistDialog = false },
            onItemSelected = { index ->
                showPlaylistDialog = false
                onPlaylistItemSelected(index)
            },
            onFavoriteSelected = { index ->
                showPlaylistDialog = false
                onPlaylistFavoriteSelected(index)
            },
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            message = "确定要删除《${title.ifBlank { "当前影片" }}》吗？",
            deleting = deleteInProgress,
            onDismiss = {
                if (!deleteInProgress) showDeleteDialog = false
            },
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            confirmEnabled = deleteEnabled,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerTopOverlay(
    title: String,
    currentTime: String,
    batteryLevel: Int,
    resizeMode: VideoResizeMode,
    isFavorite: Boolean,
    favoriteEnabled: Boolean,
    deleteEnabled: Boolean,
    classifyEnabled: Boolean,
    showPlaylistButton: Boolean,
    onBack: () -> Unit,
    onOpenPlaylist: () -> Unit,
    onCycleResizeMode: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClassify: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PlayerOverlayIconButton(
            onClick = onBack,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        Text(
            text = title,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .weight(1f)
                .basicMarquee(iterations = Int.MAX_VALUE)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (showPlaylistButton) {
                PlayerOverlayIconButton(
                    onClick = onOpenPlaylist,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.List,
                        contentDescription = "Playlist",
                        tint = Color.White,
                    )
                }
            }
            PlayerOverlayIconButton(
                onClick = onCycleResizeMode,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = resizeMode.icon,
                    contentDescription = "Resize mode ${resizeMode.label}",
                    tint = Color.White,
                )
            }
            PlayerOverlayIconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp),
                enabled = favoriteEnabled,
                containerColor = if (isFavorite) Color(0xCCB83A53) else Color(0x66202020)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                    tint = Color.White,
                )
            }
            PlayerOverlayIconButton(
                onClick = onClassify,
                modifier = Modifier.size(36.dp),
                enabled = classifyEnabled,
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalFlorist,
                    contentDescription = "Classify",
                    tint = Color.White,
                )
            }
            PlayerOverlayIconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp),
                enabled = deleteEnabled,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = currentTime,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
                BatteryLevelBadge(level = batteryLevel)
            }
        }
    }
}

@Composable
private fun PlayerOverlayIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Color(0x66202020),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                color = if (enabled) containerColor else containerColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
    }
}

@Composable
private fun FastModeIndicator(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color(0x99000000),
    ) {
        Text(
            text = "3x快进中",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PlayerChromeScrims(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(104.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.42f),
                            Color.Transparent,
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(132.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.46f),
                        )
                    )
                )
        )
    }
}

@Composable
private fun PlayerLockButton(
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .size(42.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = Color(0x66000000),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                contentDescription = if (locked) "Unlock controls" else "Lock controls",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun PlaybackRetryIndicator(
    retryAttempt: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color(0x99000000),
    ) {
        Text(
            text = "重新连接中 $retryAttempt/$PLAYBACK_RETRY_LIMIT",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SeekPreviewIndicator(
    targetPositionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color(0x99000000),
    ) {
        Text(
            text = "${formatPlayerTime(targetPositionMs)}/${formatPlayerTime(durationMs)}",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun PlaylistDialog(
    playlist: List<PlayerPlaylistItem>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onItemSelected: (Int) -> Unit,
    onFavoriteSelected: (Int) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val configuration = LocalConfiguration.current
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            val horizontalPadding = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                maxWidth * 0.15f
            } else {
                maxWidth * 0.10f
            }
            val verticalPadding = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                maxHeight * 0.10f
            } else {
                maxHeight * 0.15f
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                    .clickable { },
                shape = RoundedCornerShape(18.dp),
                color = Color(0xCC3A3A3A),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    itemsIndexed(playlist) { index, item ->
                        val selected = index == currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemSelected(index) }
                                .background(
                                    if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.title.ifBlank { item.deleteLabel },
                                modifier = Modifier.weight(1f),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            IconButton(
                                onClick = { onFavoriteSelected(index) },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    imageVector = if (item.isFavorite) {
                                        Icons.Rounded.Favorite
                                    } else {
                                        Icons.Rounded.FavoriteBorder
                                    },
                                    contentDescription = if (item.isFavorite) "取消收藏" else "收藏",
                                    tint = if (item.isFavorite) Color(0xFFFF6B8A) else Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryLevelBadge(
    level: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(end = 3.dp)
                .size(width = 24.dp, height = 12.dp)
                .background(Color.Transparent, RoundedCornerShape(3.dp))
                .border(
                    width = 1.dp,
                    color = batteryTint(level),
                    shape = RoundedCornerShape(3.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = level.coerceIn(0, 100).toString(),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                lineHeight = 8.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 24.dp)
                .size(width = 2.dp, height = 5.dp)
                .background(batteryTint(level), RoundedCornerShape(1.dp))
        )
    }
}

@Composable
private fun PlayerBottomControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 42.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent, CircleShape)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

@Composable
private fun SideLevelIndicator(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    levelFraction: Float,
    levelPercent: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(16.dp)
            )
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 92.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(999.dp))
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(levelFraction.coerceIn(0f, 1f))
                        .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(999.dp))
                )
            }
        }
        Box(
            modifier = Modifier.size(width = 24.dp, height = 20.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = levelPercent.toString(),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun VideoControlsOverlay(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    canPlayPrevious: Boolean,
    onPlayPrevious: () -> Unit,
    canPlayNext: Boolean,
    onPlayNext: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekEnd: () -> Unit,
    playbackSpeed: Float,
    onPlaybackSpeedSelected: (Float) -> Unit,
    onToggleOrientation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeDurationMs = durationMs.takeIf { it > 0 } ?: 1L

    BoxWithConstraints(modifier = modifier) {
        val compactControls = maxWidth < 380.dp
        val showInlineSpeeds = maxWidth >= 520.dp
        val buttonSize = if (compactControls) 36.dp else 42.dp
        val timeTextStyle = if (compactControls) {
            MaterialTheme.typography.labelSmall
        } else {
            MaterialTheme.typography.labelMedium
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (compactControls) 0.dp else 1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatPlayerTime(positionMs),
                    color = Color.White,
                    style = timeTextStyle,
                    fontWeight = FontWeight.Medium,
                )
                VideoSeekBar(
                    positionMs = positionMs,
                    durationMs = safeDurationMs,
                    onSeekChange = onSeekChange,
                    onSeekEnd = onSeekEnd,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = if (compactControls) 6.dp else 10.dp)
                )
                Text(
                    text = formatPlayerTime(durationMs),
                    color = Color.White,
                    style = timeTextStyle,
                    fontWeight = FontWeight.Medium,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerBottomControlButton(
                    onClick = onPlayPause,
                    size = buttonSize,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(if (compactControls) 20.dp else 22.dp),
                    )
                }

                if (canPlayPrevious) {
                    PlayerBottomControlButton(
                        onClick = onPlayPrevious,
                        size = buttonSize,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous video",
                            tint = Color.White,
                            modifier = Modifier.size(if (compactControls) 20.dp else 22.dp),
                        )
                    }
                }

                if (canPlayNext) {
                    PlayerBottomControlButton(
                        onClick = onPlayNext,
                        size = buttonSize,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next video",
                            tint = Color.White,
                            modifier = Modifier.size(if (compactControls) 20.dp else 22.dp),
                        )
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpeedSelector(
                        playbackSpeed = playbackSpeed,
                        showInlineSpeeds = showInlineSpeeds,
                        compact = compactControls,
                        onPlaybackSpeedSelected = onPlaybackSpeedSelected,
                    )

                    PlayerBottomControlButton(
                        onClick = onToggleOrientation,
                        size = buttonSize,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ScreenRotationAlt,
                            contentDescription = "Rotate screen",
                            tint = Color.White,
                            modifier = Modifier.size(if (compactControls) 20.dp else 22.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedSelector(
    playbackSpeed: Float,
    showInlineSpeeds: Boolean,
    compact: Boolean,
    onPlaybackSpeedSelected: (Float) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    if (showInlineSpeeds) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            playbackSpeeds.forEach { speed ->
                val selected = kotlin.math.abs(speed - playbackSpeed) < 0.01f
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) Color.White.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .size(width = 44.dp, height = 32.dp)
                        .clickable { onPlaybackSpeedSelected(speed) },
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = formatPlaybackSpeed(speed),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    } else {
        Box {
            PlayerBottomControlButton(
                onClick = { menuExpanded = true },
                size = if (compact) 36.dp else 42.dp,
            ) {
                Text(
                    text = formatPlaybackSpeed(playbackSpeed),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    fontSize = if (compact) 12.sp else 14.sp,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                playbackSpeeds.forEach { speed ->
                    DropdownMenuItem(
                        text = { Text(formatPlaybackSpeed(speed)) },
                        onClick = {
                            menuExpanded = false
                            onPlaybackSpeedSelected(speed)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeekChange: (Float) -> Unit,
    onSeekEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var trackSize by remember { mutableStateOf(IntSize.Zero) }
    val fraction = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val thumbRadius = 4.dp
    val thumbDiameter = thumbRadius * 2
    val trackWidth = with(density) { trackSize.width.toDp() }
    val thumbOffset = with(density) { (trackSize.width * fraction).toDp() - thumbRadius }
        .coerceIn(0.dp, (trackWidth - thumbDiameter).coerceAtLeast(0.dp))

    Box(
        modifier = modifier
            .height(16.dp)
            .onSizeChanged { trackSize = it }
            .pointerInput(durationMs, trackSize) {
                detectTapGestures { offset ->
                    if (trackSize.width <= 0) return@detectTapGestures
                    val target = (offset.x / trackSize.width.toFloat()).coerceIn(0f, 1f) * durationMs
                    onSeekChange(target)
                    onSeekEnd()
                }
            }
            .pointerInput(durationMs, trackSize) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (trackSize.width <= 0) return@detectHorizontalDragGestures
                        val target = (offset.x / trackSize.width.toFloat()).coerceIn(0f, 1f) * durationMs
                        onSeekChange(target)
                    },
                    onHorizontalDrag = { change, _ ->
                        if (trackSize.width <= 0) return@detectHorizontalDragGestures
                        val target = (change.position.x / trackSize.width.toFloat()).coerceIn(0f, 1f) * durationMs
                        onSeekChange(target)
                    },
                    onDragEnd = onSeekEnd,
                )
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.28f), RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(fraction)
                .height(2.dp)
                .background(Color.White, RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = thumbOffset)
                .size(thumbDiameter)
                .background(Color.White, RoundedCornerShape(50))
        )
    }
}

private fun formatPlayerTime(timeMs: Long): String {
    val totalSeconds = (timeMs.coerceAtLeast(0L) / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun formatPlaybackSpeed(speed: Float): String {
    return if (speed % 1f == 0f) {
        "${speed.toInt()}x"
    } else {
        "${speed}x"
    }
}

private fun currentTimeText(): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}

private fun readBatteryLevel(context: Context): Int {
    val batteryManager = context.getSystemService(BatteryManager::class.java)
    val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
    if (level >= 0) return level
    val status = context.registerReceiver(
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    )
    val currentLevel = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    return if (currentLevel >= 0 && scale > 0) ((currentLevel * 100f) / scale).toInt() else 100
}

private fun batteryTint(level: Int): Color {
    return when {
        level <= 15 -> Color(0xFFFF6B6B)
        level <= 35 -> Color(0xFFFFC857)
        else -> Color.White
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private enum class PlayerGestureTarget {
    Seek,
    Brightness,
    Volume,
}

private const val PLAYER_PROGRESS_PREFS = "player_progress"
private const val RESUME_MIN_DURATION_MS = 60_000L
private const val RESUME_MIN_POSITION_MS = 10_000L
private const val RESUME_END_CLEAR_WINDOW_MS = 15_000L
private const val PROGRESS_SAVE_INTERVAL_MS = 5_000L
private const val PLAYBACK_RETRY_LIMIT = 2
private const val PLAYBACK_RETRY_DELAY_MS = 1_200L

private val playbackSpeeds = listOf(0.5f, 0.75f, 1f, 1.5f, 2f, 3f, 4f, 5f)

private enum class VideoResizeMode(
    val label: String,
    val playerViewMode: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Fit(
        label = "Normal",
        playerViewMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
        icon = Icons.Rounded.FitScreen,
    ),
    Crop(
        label = "Tile",
        playerViewMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        icon = Icons.Rounded.CropFree,
    ),
    Fill(
        label = "Stretch",
        playerViewMode = AspectRatioFrameLayout.RESIZE_MODE_FILL,
        icon = Icons.Rounded.OpenInFull,
    );

    fun next(): VideoResizeMode {
        val all = values()
        return all[(ordinal + 1) % all.size]
    }
}

@Composable
private fun PlayerSurface(
    exoPlayer: ExoPlayer,
    resizeMode: Int,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                player = exoPlayer
                useController = false
                this.resizeMode = resizeMode
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                setBackgroundColor(android.graphics.Color.BLACK)
                setShowPreviousButton(false)
                setShowNextButton(false)
                setShowRewindButton(false)
                setShowFastForwardButton(false)
                setShowSubtitleButton(false)
            }
        },
        modifier = modifier,
        update = { playerView ->
            playerView.player = exoPlayer
            playerView.resizeMode = resizeMode
        }
    )
}

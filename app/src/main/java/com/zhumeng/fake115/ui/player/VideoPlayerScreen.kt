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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenRotationAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onBack: () -> Unit,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isFullScreen by rememberSaveable { mutableStateOf(false) }
    var resolvedUrl by rememberSaveable { mutableStateOf(initialUrl) }
    var isResolving by rememberSaveable { mutableStateOf(initialUrl.isBlank() && pc.isNotBlank()) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isFavorite by rememberSaveable(itemId) { mutableStateOf(initialFavorite) }
    var isFavoriteUpdating by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val exoPlayer = rememberManagedExoPlayer(
        url = resolvedUrl,
        requestHeaders = requestHeaders,
    )

    LaunchedEffect(initialUrl, pc) {
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
                    }
                    .onFailure { error ->
                        errorMessage = error.message ?: "Failed to update favorite."
                    }
                isFavoriteUpdating = false
            }
        }
    }

    val handleDelete: () -> Unit = {
        if (deleteVideo != null && itemId.isNotBlank() && !isDeleting) {
            scope.launch {
                isDeleting = true
                runCatching { deleteVideo(itemId) }
                    .onSuccess {
                        onBack()
                    }
                    .onFailure { error ->
                        errorMessage = error.message ?: "Failed to delete video."
                        isDeleting = false
                    }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF050914)
    ) {
        if (isLandscape) {
            when {
                isResolving -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = errorMessage ?: "Playback failed.",
                            color = Color.White,
                        )
                    }
                }

                else -> {
                    exoPlayer?.let {
                        EmbeddedVideoPlayer(
                            exoPlayer = it,
                            modifier = Modifier.fillMaxSize(),
                            isFullScreen = true,
                            onToggleFullScreen = {},
                            isFavorite = isFavorite,
                            favoriteEnabled = !isFavoriteUpdating && updateFavorite != null && itemId.isNotBlank(),
                            deleteEnabled = !isDeleting && deleteVideo != null && itemId.isNotBlank(),
                            deleteInProgress = isDeleting,
                            onToggleFavorite = handleToggleFavorite,
                            onDelete = handleDelete,
                            deleteLabel = deleteLabel,
                            title = title.ifBlank { "Video" },
                            onBack = onBack,
                            showTopChrome = true,
                            showFullScreenButton = false,
                            forceFullScreen = true,
                        )
                    }
                }
            }
            return@Surface
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                isResolving -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Resolving playback url...")
                        }
                    }
                }

                errorMessage != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = errorMessage ?: "Playback failed.", color = Color.White)
                            Button(
                                onClick = {
                                    if (pc.isBlank() || resolveUrl == null) return@Button
                                    scope.launch {
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
                                }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                else -> {
                    exoPlayer?.let {
                        EmbeddedVideoPlayer(
                            exoPlayer = it,
                            modifier = Modifier.fillMaxWidth(),
                            isFullScreen = isFullScreen,
                            onToggleFullScreen = { isFullScreen = !isFullScreen },
                            isFavorite = isFavorite,
                            favoriteEnabled = !isFavoriteUpdating && updateFavorite != null && itemId.isNotBlank(),
                            deleteEnabled = !isDeleting && deleteVideo != null && itemId.isNotBlank(),
                            deleteInProgress = isDeleting,
                            onToggleFavorite = handleToggleFavorite,
                            onDelete = handleDelete,
                            deleteLabel = deleteLabel,
                            title = title.ifBlank { "Video" },
                            onBack = onBack,
                            showTopChrome = true,
                            showFullScreenButton = false,
                        )
                    }
                }
            }
        }
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
    deleteInProgress: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onDelete: () -> Unit = {},
    title: String = "",
    deleteLabel: String = "",
    onBack: () -> Unit = {},
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
                    deleteInProgress = deleteInProgress,
                    onToggleFavorite = onToggleFavorite,
                    onDelete = onDelete,
                    title = title,
                    deleteLabel = deleteLabel,
                    onBack = onBack,
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
                            deleteInProgress = deleteInProgress,
                            onToggleFavorite = onToggleFavorite,
                            onDelete = onDelete,
                            title = title,
                            deleteLabel = deleteLabel,
                            onBack = onBack,
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
                    deleteInProgress = deleteInProgress,
                    onToggleFavorite = onToggleFavorite,
                    onDelete = onDelete,
                    title = title,
                    deleteLabel = deleteLabel,
                    onBack = onBack,
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
    deleteInProgress: Boolean,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    title: String,
    deleteLabel: String,
    onBack: () -> Unit,
    showTopChrome: Boolean,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isFastModeActive by remember { mutableStateOf(false) }
    var previousControlsVisible by remember { mutableStateOf(true) }
    var skipNextTap by remember { mutableStateOf(false) }
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

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = currentTimeText()
            batteryLevel = readBatteryLevel(context)
            delay(1000)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
                if (!isSeeking) {
                    positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                }
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                playbackSpeed = playbackParameters.speed
            }
        }

        exoPlayer.addListener(listener)
        durationMs = exoPlayer.duration.coerceAtLeast(0L)
        positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
        isPlaying = exoPlayer.isPlaying
        playbackSpeed = exoPlayer.playbackParameters.speed
        onDispose {
            exoPlayer.removeListener(listener)
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

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(exoPlayer, containerSize) {
                detectTapGestures(
                    onPress = { offset ->
                        if (containerSize.width <= 0 || offset.x < containerSize.width * (2f / 3f)) {
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
                        }
                    },
                    onTap = { _ ->
                        if (skipNextTap) {
                            skipNextTap = false
                            return@detectTapGestures
                        }
                        controlsVisible = !controlsVisible
                    },
                    onDoubleTap = { _ ->
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    }
                )
            }
            .pointerInput(audioManager) {
                var initialTouchX = 0f
                var accumulatedX = 0f
                var accumulatedY = 0f
                var lockedTarget: SideAdjustTarget? = null
                var gestureRejected = false

                detectDragGestures(
                    onDragStart = { offset ->
                        initialTouchX = offset.x
                        accumulatedX = 0f
                        accumulatedY = 0f
                        lockedTarget = null
                        gestureRejected = false
                    },
                    onDrag = { change, dragAmount ->
                        if (size.height <= 0 || size.width <= 0) return@detectDragGestures

                        accumulatedX += dragAmount.x
                        accumulatedY += dragAmount.y

                        if (lockedTarget == null && !gestureRejected) {
                            val absX = kotlin.math.abs(accumulatedX)
                            val absY = kotlin.math.abs(accumulatedY)

                            if (maxOf(absX, absY) < 12f) {
                                return@detectDragGestures
                            }

                            if (absY > absX) {
                                lockedTarget = if (initialTouchX <= size.width / 2f) {
                                    SideAdjustTarget.Brightness
                                } else {
                                    SideAdjustTarget.Volume
                                }
                            } else {
                                gestureRejected = true
                                return@detectDragGestures
                            }
                        }

                        val dragRatio = (-dragAmount.y / size.height.toFloat()).coerceIn(-1f, 1f)
                        when (lockedTarget) {
                            SideAdjustTarget.Brightness -> {
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

                            SideAdjustTarget.Volume -> {
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
                        initialTouchX = 0f
                        accumulatedX = 0f
                        accumulatedY = 0f
                        lockedTarget = null
                        gestureRejected = false
                    },
                    onDragCancel = {
                        initialTouchX = 0f
                        accumulatedX = 0f
                        accumulatedY = 0f
                        lockedTarget = null
                        gestureRejected = false
                    }
                )
            }
    ) {
        PlayerSurface(
            exoPlayer = exoPlayer,
            resizeMode = resizeMode.playerViewMode,
            modifier = Modifier.matchParentSize()
        )

        if (showTopChrome && controlsVisible && !isFastModeActive) {
            PlayerTopOverlay(
                title = title,
                currentTime = currentTime,
                batteryLevel = batteryLevel,
                resizeMode = resizeMode,
                isFavorite = isFavorite,
                favoriteEnabled = favoriteEnabled,
                deleteEnabled = deleteEnabled,
                onBack = onBack,
                onCycleResizeMode = {
                    resizeMode = resizeMode.next()
                },
                onToggleFavorite = onToggleFavorite,
                onDelete = {
                    showDeleteDialog = true
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }

        if (controlsVisible && !isFastModeActive) {
            VideoControlsOverlay(
                isPlaying = isPlaying,
                positionMs = if (isSeeking) sliderPositionMs.toLong() else positionMs,
                durationMs = durationMs,
                onPlayPause = {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                },
                onSeekChange = { value ->
                    if (!isSeeking) {
                        isSeeking = true
                    }
                    sliderPositionMs = value
                },
                onSeekEnd = {
                    exoPlayer.seekTo(sliderPositionMs.toLong())
                    positionMs = sliderPositionMs.toLong()
                    isSeeking = false
                },
                playbackSpeed = playbackSpeed,
                onCyclePlaybackSpeed = {
                    val nextSpeed = playbackSpeeds.nextAfter(playbackSpeed)
                    exoPlayer.setPlaybackParameters(PlaybackParameters(nextSpeed))
                    playbackSpeed = nextSpeed
                },
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
                    .padding(start = 12.dp)
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                if (deleteInProgress) return@AlertDialog
                showDeleteDialog = false
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                    },
                    enabled = deleteEnabled,
                ) {
                    Text(if (deleteEnabled) "确认删除" else "删除中...")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = deleteEnabled,
                ) {
                    Text("取消")
                }
            },
            title = {
                Text("确认删除")
            },
            text = {
                Text("确定要删除《${title.ifBlank { "当前影片" }}》吗？")
            }
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
    onBack: () -> Unit,
    onCycleResizeMode: () -> Unit,
    onToggleFavorite: () -> Unit,
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
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier.size(42.dp),
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
    onSeekChange: (Float) -> Unit,
    onSeekEnd: () -> Unit,
    playbackSpeed: Float,
    onCyclePlaybackSpeed: () -> Unit,
    onToggleOrientation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeDurationMs = durationMs.takeIf { it > 0 } ?: 1L

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatPlayerTime(positionMs),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
            VideoSeekBar(
                positionMs = positionMs,
                durationMs = safeDurationMs,
                onSeekChange = onSeekChange,
                onSeekEnd = onSeekEnd,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            )
            Text(
                text = formatPlayerTime(durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerBottomControlButton(
                onClick = onPlayPause
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PlayerBottomControlButton(
                    onClick = onCyclePlaybackSpeed
                ) {
                    Text(
                        text = formatPlaybackSpeed(playbackSpeed),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                }
                PlayerBottomControlButton(
                    onClick = onToggleOrientation
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ScreenRotationAlt,
                        contentDescription = "Rotate screen",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
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

private fun List<Float>.nextAfter(current: Float): Float {
    val currentIndex = indexOfFirst { kotlin.math.abs(it - current) < 0.01f }
    return if (currentIndex == -1 || currentIndex == lastIndex) {
        first()
    } else {
        this[currentIndex + 1]
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

private enum class SideAdjustTarget {
    Brightness,
    Volume,
}

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

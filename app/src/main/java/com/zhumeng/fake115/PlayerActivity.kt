package com.zhumeng.fake115

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import com.zhumeng.fake115.data.LibraryRepository
import com.zhumeng.fake115.data.NetDiskRepository
import com.zhumeng.fake115.data.model.LibraryMovie
import com.zhumeng.fake115.data.model.NetDiskFile
import com.zhumeng.fake115.ui.player.PlayerPlaylistItem
import com.zhumeng.fake115.ui.player.VideoPlayerScreen
import com.zhumeng.fake115.ui.theme.Fake115Theme

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applyImmersivePlayerMode()

        val fallbackVideoId = intent.getIntExtra(EXTRA_VIDEO_ID, 0)
        val fallbackItemId = intent.getStringExtra(EXTRA_ITEM_ID).orEmpty().ifBlank {
            fallbackVideoId.takeIf { it > 0 }?.toString().orEmpty()
        }
        val fallbackTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val fallbackDeleteLabel = intent.getStringExtra(EXTRA_DELETE_LABEL).orEmpty()
        val fallbackPc = intent.getStringExtra(EXTRA_PC).orEmpty()
        val fallbackFavorite = intent.getBooleanExtra(EXTRA_IS_FAVORITE, false)
        val useNetDiskActions = intent.getBooleanExtra(EXTRA_USE_NET_DISK_ACTIONS, false)
        val removeFromPlaylistOnFavorite = intent.getBooleanExtra(EXTRA_REMOVE_FROM_PLAYLIST_ON_FAVORITE, false)
        val initialPlaylist = readPlaylist(intent).ifEmpty {
            listOf(
                PlayerPlaylistItem(
                    itemId = fallbackItemId,
                    title = fallbackTitle,
                    deleteLabel = fallbackDeleteLabel,
                    pc = fallbackPc,
                    isFavorite = fallbackFavorite,
                )
            )
        }
        val initialIndex = initialPlaylist.indexOfFirst { it.itemId == fallbackItemId }
            .takeIf { it >= 0 } ?: 0
        val libraryRepository = LibraryRepository(applicationContext)
        val netDiskRepository = NetDiskRepository(applicationContext)

        setContent {
            Fake115Theme {
                var playlist by remember { mutableStateOf(initialPlaylist) }
                var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, playlist.lastIndex)) }
                val currentItem = playlist.getOrNull(currentIndex)

                if (currentItem == null) {
                    finish()
                    } else {
                    VideoPlayerScreen(
                        itemId = currentItem.itemId,
                        title = currentItem.title,
                        deleteLabel = currentItem.deleteLabel,
                        initialUrl = "",
                        pc = currentItem.pc,
                        initialFavorite = currentItem.isFavorite,
                        autoPlayNextAfterFavorite = useNetDiskActions && !removeFromPlaylistOnFavorite,
                        playlist = if (playlist.size <= 1) emptyList() else playlist,
                        currentPlaylistIndex = currentIndex,
                        onPlaylistItemSelected = { index ->
                            if (index in playlist.indices) {
                                currentIndex = index
                            }
                        },
                        onPlaybackEnded = {
                            if (currentIndex < playlist.lastIndex) {
                                currentIndex += 1
                            }
                        },
                        requestHeaders = libraryRepository.build115RequestHeaders(),
                        resolveUrl = libraryRepository::resolve115PlayableUrl,
                        updateFavorite = if (useNetDiskActions) {
                            { id, favorite ->
                                val updated = netDiskRepository.updateStar(id, favorite)
                                if (removeFromPlaylistOnFavorite && updated) {
                                    val removedIndex = playlist.indexOfFirst { it.itemId == id }
                                    if (removedIndex >= 0) {
                                        val wasCurrent = removedIndex == currentIndex
                                        playlist = playlist.filterNot { it.itemId == id }
                                        currentIndex = when {
                                            playlist.isEmpty() -> 0
                                            wasCurrent -> removedIndex.coerceAtMost(playlist.lastIndex)
                                            removedIndex < currentIndex -> currentIndex - 1
                                            else -> currentIndex
                                        }
                                    }
                                } else {
                                    playlist = playlist.map { item ->
                                        if (item.itemId == id) item.copy(isFavorite = updated) else item
                                    }
                                }
                                updated
                            }
                        } else {
                            { id, favorite ->
                                val updated = libraryRepository.updateFavorite(id.toInt(), favorite)
                                playlist = playlist.map { item ->
                                    if (item.itemId == id) item.copy(isFavorite = updated) else item
                                }
                                updated
                            }
                        },
                        deleteVideo = if (useNetDiskActions) {
                            { id ->
                                val message = netDiskRepository.deleteFile(id)
                                NetDiskRepository.notifyFileDeleted(id)
                                message
                            }
                        } else {
                            { id -> libraryRepository.deleteMovie(id.toInt()) }
                        },
                        classifyVideo = if (useNetDiskActions) {
                            { id ->
                                val message = netDiskRepository.moveFile(
                                    fileId = id,
                                    targetCid = NetDiskRepository.CLASSIFIED_TARGET_CID,
                                )
                                NetDiskRepository.notifyFileDeleted(id)
                                message
                            }
                        } else {
                            null
                        },
                        onDeleteCompleted = {
                            if (playlist.size <= 1) {
                                finish()
                            } else {
                                val removedIndex = currentIndex
                                val nextPlaylist = playlist.toMutableList().apply {
                                    removeAt(removedIndex)
                                }
                                playlist = nextPlaylist
                                currentIndex = removedIndex.coerceAtMost(nextPlaylist.lastIndex)
                            }
                        },
                        onBack = ::finish
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyImmersivePlayerMode()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersivePlayerMode()
        }
    }

    private fun applyImmersivePlayerMode() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
    }

    private fun readPlaylist(intent: Intent): List<PlayerPlaylistItem> {
        val ids = intent.getStringArrayListExtra(EXTRA_PLAYLIST_IDS).orEmpty()
        val titles = intent.getStringArrayListExtra(EXTRA_PLAYLIST_TITLES).orEmpty()
        val deleteLabels = intent.getStringArrayListExtra(EXTRA_PLAYLIST_DELETE_LABELS).orEmpty()
        val pcs = intent.getStringArrayListExtra(EXTRA_PLAYLIST_PCS).orEmpty()
        val favorites = intent.getBooleanArrayExtra(EXTRA_PLAYLIST_FAVORITES) ?: BooleanArray(0)

        return ids.mapIndexedNotNull { index, id ->
            val pc = pcs.getOrNull(index).orEmpty()
            if (id.isBlank() || pc.isBlank()) return@mapIndexedNotNull null
            PlayerPlaylistItem(
                itemId = id,
                title = titles.getOrNull(index).orEmpty(),
                deleteLabel = deleteLabels.getOrNull(index).orEmpty(),
                pc = pc,
                isFavorite = favorites.getOrNull(index) ?: false,
            )
        }
    }

    companion object {
        private const val EXTRA_VIDEO_ID = "extra_video_id"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_DELETE_LABEL = "extra_delete_label"
        private const val EXTRA_PC = "extra_pc"
        private const val EXTRA_IS_FAVORITE = "extra_is_favorite"
        private const val EXTRA_ITEM_ID = "extra_item_id"
        private const val EXTRA_USE_NET_DISK_ACTIONS = "extra_use_net_disk_actions"
        private const val EXTRA_PLAYLIST_IDS = "extra_playlist_ids"
        private const val EXTRA_PLAYLIST_TITLES = "extra_playlist_titles"
        private const val EXTRA_PLAYLIST_DELETE_LABELS = "extra_playlist_delete_labels"
        private const val EXTRA_PLAYLIST_PCS = "extra_playlist_pcs"
        private const val EXTRA_PLAYLIST_FAVORITES = "extra_playlist_favorites"
        private const val EXTRA_REMOVE_FROM_PLAYLIST_ON_FAVORITE = "extra_remove_from_playlist_on_favorite"
        private const val MAX_INTENT_PLAYLIST_ITEMS = 150

        fun createIntent(
            context: Context,
            videoId: Int,
            title: String,
            deleteLabel: String,
            pc: String,
            isFavorite: Boolean,
            playlist: List<LibraryMovie> = emptyList(),
        ): Intent {
            val safePlaylist = playlist.takeIntentWindow { it.id == videoId }
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_ID, videoId)
                putExtra(EXTRA_ITEM_ID, videoId.toString())
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_DELETE_LABEL, deleteLabel)
                putExtra(EXTRA_PC, pc)
                putExtra(EXTRA_IS_FAVORITE, isFavorite)
                putStringArrayListExtra(
                    EXTRA_PLAYLIST_IDS,
                    ArrayList(safePlaylist.map { it.id.toString() }),
                )
                putStringArrayListExtra(
                    EXTRA_PLAYLIST_TITLES,
                    ArrayList(
                        safePlaylist.map { movie ->
                            listOf(movie.fanhao, movie.name)
                                .filter { it.isNotBlank() }
                                .joinToString(" ")
                                .ifBlank { movie.fanhao.ifBlank { movie.name } }
                        }
                    ),
                )
                putStringArrayListExtra(
                    EXTRA_PLAYLIST_DELETE_LABELS,
                    ArrayList(safePlaylist.map { it.fanhao.ifBlank { it.name } }),
                )
                putStringArrayListExtra(EXTRA_PLAYLIST_PCS, ArrayList(safePlaylist.map { it.pc }))
                putExtra(EXTRA_PLAYLIST_FAVORITES, safePlaylist.map { it.isFavorite == 1 }.toBooleanArray())
            }
        }

        fun createNetDiskIntent(
            context: Context,
            fileId: String,
            title: String,
            deleteLabel: String,
            pc: String,
            isFavorite: Boolean,
            playlist: List<NetDiskFile> = emptyList(),
            removeFromPlaylistOnFavorite: Boolean = false,
        ): Intent {
            val safePlaylist = playlist.takeIntentWindow { it.id == fileId }
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_ITEM_ID, fileId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_DELETE_LABEL, deleteLabel)
                putExtra(EXTRA_PC, pc)
                putExtra(EXTRA_IS_FAVORITE, isFavorite)
                putExtra(EXTRA_USE_NET_DISK_ACTIONS, true)
                putExtra(EXTRA_REMOVE_FROM_PLAYLIST_ON_FAVORITE, removeFromPlaylistOnFavorite)
                putStringArrayListExtra(
                    EXTRA_PLAYLIST_IDS,
                    ArrayList(safePlaylist.map { it.id }),
                )
                putStringArrayListExtra(
                    EXTRA_PLAYLIST_TITLES,
                    ArrayList(safePlaylist.map { it.n }),
                )
                putStringArrayListExtra(
                    EXTRA_PLAYLIST_DELETE_LABELS,
                    ArrayList(safePlaylist.map { it.n }),
                )
                putStringArrayListExtra(
                    EXTRA_PLAYLIST_PCS,
                    ArrayList(safePlaylist.map { it.pc.orEmpty() }),
                )
                putExtra(EXTRA_PLAYLIST_FAVORITES, safePlaylist.map { it.isStarred }.toBooleanArray())
            }
        }

        private fun <T> List<T>.takeIntentWindow(isCurrent: (T) -> Boolean): List<T> {
            if (size <= MAX_INTENT_PLAYLIST_ITEMS) return this
            val currentIndex = indexOfFirst(isCurrent)
            if (currentIndex < 0) return take(MAX_INTENT_PLAYLIST_ITEMS)
            val half = MAX_INTENT_PLAYLIST_ITEMS / 2
            val start = (currentIndex - half).coerceIn(0, (size - MAX_INTENT_PLAYLIST_ITEMS).coerceAtLeast(0))
            return subList(start, (start + MAX_INTENT_PLAYLIST_ITEMS).coerceAtMost(size))
        }
    }
}

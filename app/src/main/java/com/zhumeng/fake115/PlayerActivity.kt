package com.zhumeng.fake115

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import com.zhumeng.fake115.data.LibraryRepository
import com.zhumeng.fake115.data.NetDiskRepository
import com.zhumeng.fake115.ui.player.VideoPlayerScreen
import com.zhumeng.fake115.ui.theme.Fake115Theme

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applyImmersivePlayerMode()

        val videoId = intent.getIntExtra(EXTRA_VIDEO_ID, 0)
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID).orEmpty().ifBlank {
            videoId.takeIf { it > 0 }?.toString().orEmpty()
        }
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val deleteLabel = intent.getStringExtra(EXTRA_DELETE_LABEL).orEmpty()
        val pc = intent.getStringExtra(EXTRA_PC).orEmpty()
        val initialFavorite = intent.getBooleanExtra(EXTRA_IS_FAVORITE, false)
        val useNetDiskActions = intent.getBooleanExtra(EXTRA_USE_NET_DISK_ACTIONS, false)
        val libraryRepository = LibraryRepository(applicationContext)
        val netDiskRepository = NetDiskRepository(applicationContext)

        setContent {
            Fake115Theme {
                VideoPlayerScreen(
                    itemId = itemId,
                    title = title,
                    deleteLabel = deleteLabel,
                    initialUrl = "",
                    pc = pc,
                    initialFavorite = initialFavorite,
                    requestHeaders = libraryRepository.build115RequestHeaders(),
                    resolveUrl = libraryRepository::resolve115PlayableUrl,
                    updateFavorite = if (useNetDiskActions) {
                        { id, favorite -> netDiskRepository.updateStar(id, favorite) }
                    } else {
                        { id, favorite -> libraryRepository.updateFavorite(id.toInt(), favorite) }
                    },
                    deleteVideo = if (useNetDiskActions) {
                        netDiskRepository::deleteFile
                    } else {
                        { id -> libraryRepository.deleteMovie(id.toInt()) }
                    },
                    onBack = ::finish
                )
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

    companion object {
        private const val EXTRA_VIDEO_ID = "extra_video_id"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_DELETE_LABEL = "extra_delete_label"
        private const val EXTRA_PC = "extra_pc"
        private const val EXTRA_IS_FAVORITE = "extra_is_favorite"
        private const val EXTRA_ITEM_ID = "extra_item_id"
        private const val EXTRA_USE_NET_DISK_ACTIONS = "extra_use_net_disk_actions"

        fun createIntent(
            context: Context,
            videoId: Int,
            title: String,
            deleteLabel: String,
            pc: String,
            isFavorite: Boolean,
        ): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_ID, videoId)
                putExtra(EXTRA_ITEM_ID, videoId.toString())
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_DELETE_LABEL, deleteLabel)
                putExtra(EXTRA_PC, pc)
                putExtra(EXTRA_IS_FAVORITE, isFavorite)
            }
        }

        fun createNetDiskIntent(
            context: Context,
            fileId: String,
            title: String,
            deleteLabel: String,
            pc: String,
            isFavorite: Boolean,
        ): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_ITEM_ID, fileId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_DELETE_LABEL, deleteLabel)
                putExtra(EXTRA_PC, pc)
                putExtra(EXTRA_IS_FAVORITE, isFavorite)
                putExtra(EXTRA_USE_NET_DISK_ACTIONS, true)
            }
        }
    }
}

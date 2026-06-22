package com.zhumeng.fake115

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.zhumeng.fake115.data.LibraryRepository
import com.zhumeng.fake115.ui.detail.VideoDetailScreen
import com.zhumeng.fake115.ui.theme.Fake115Theme

class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val videoId = intent.getIntExtra(EXTRA_VIDEO_ID, 0)
        val repository = LibraryRepository(applicationContext)

        setContent {
            Fake115Theme {
                VideoDetailScreen(
                    videoId = videoId,
                    repository = repository,
                    onBack = ::finish,
                    onFilterTag = { queryKey, value ->
                        startActivity(
                            MainActivity.createLibraryFilterIntent(
                                context = this,
                                queryKey = queryKey,
                                value = value,
                            )
                        )
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_VIDEO_ID = "extra_video_id"

        fun createIntent(
            context: Context,
            videoId: Int,
        ): Intent {
            return Intent(context, DetailActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_ID, videoId)
            }
        }
    }
}

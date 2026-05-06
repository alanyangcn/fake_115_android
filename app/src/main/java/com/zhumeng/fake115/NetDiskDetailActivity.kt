package com.zhumeng.fake115

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.zhumeng.fake115.ui.theme.AppTheme
import com.zhumeng.fake115.ui.theme.Fake115Theme

private const val DETAIL_TITLE_FALLBACK = "详情"
private const val DETAIL_BACK = "返回"
private const val DETAIL_TODO = "详情页暂未实现"

class NetDiskDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()

        setContent {
            Fake115Theme {
                NetDiskDetailScreen(
                    title = title,
                    onBack = ::finish,
                )
            }
        }
    }

    companion object {
        private const val EXTRA_ID = "extra_netdisk_id"
        private const val EXTRA_TITLE = "extra_netdisk_title"

        fun createIntent(
            context: Context,
            id: String,
            title: String,
        ): Intent {
            return Intent(context, NetDiskDetailActivity::class.java).apply {
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_TITLE, title)
            }
        }
    }
}

@Composable
private fun NetDiskDetailScreen(
    title: String,
    onBack: () -> Unit,
) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground),
    ) {
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
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = DETAIL_TODO,
                color = colors.textTertiary,
            )
        }
    }
}

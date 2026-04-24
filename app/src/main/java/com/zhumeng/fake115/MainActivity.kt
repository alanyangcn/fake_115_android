package com.zhumeng.fake115

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhumeng.fake115.data.model.LibraryMovie
import com.zhumeng.fake115.ui.actress.ActressScreen
import com.zhumeng.fake115.ui.actress.ActressViewModel
import com.zhumeng.fake115.ui.home.LibraryScreen
import com.zhumeng.fake115.ui.home.LibraryViewModel
import com.zhumeng.fake115.ui.netdisk.NetDiskScreen
import com.zhumeng.fake115.ui.netdisk.NetDiskViewModel
import com.zhumeng.fake115.ui.settings.SettingsScreen
import com.zhumeng.fake115.ui.settings.SettingsViewModel
import com.zhumeng.fake115.ui.theme.AppTheme
import com.zhumeng.fake115.ui.theme.Fake115Theme

private const val TAB_HOME = "\u9996\u9875"
private const val TAB_ACTRESS = "\u6f14\u5458"
private const val TAB_NET_DISK = "\u7f51\u76d8"
private const val TAB_SETTINGS = "\u8bbe\u7f6e"
private const val PLACEHOLDER_MOVIE = "\u641c\u7d22\u6807\u9898\u6216\u756a\u53f7"
private const val PLACEHOLDER_ACTRESS = "\u641c\u7d22\u6f14\u5458"
private const val LABEL_SEARCH = "\u641c\u7d22"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        setContent {
            Fake115Theme {
                MainScreen(
                    onOpenPlayer = { movie ->
                        startActivity(
                            PlayerActivity.createIntent(
                                context = this,
                                videoId = movie.id,
                                title = listOf(movie.fanhao, movie.name)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" ")
                                    .ifBlank { movie.fanhao },
                                deleteLabel = movie.fanhao.ifBlank { movie.name },
                                pc = movie.pc,
                                isFavorite = movie.isFavorite == 1,
                            )
                        )
                    },
                    onOpenDetail = { movie ->
                        startActivity(
                            DetailActivity.createIntent(
                                context = this,
                                videoId = movie.id,
                            )
                        )
                    },
                )
            }
        }
    }
}

private enum class MainTab {
    Home,
    Actress,
    NetDisk,
    Settings,
}

@Composable
private fun MainScreen(
    onOpenPlayer: (LibraryMovie) -> Unit,
    onOpenDetail: (LibraryMovie) -> Unit,
) {
    val colors = AppTheme.colors
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.NetDisk) }
    val libraryViewModel: LibraryViewModel = viewModel()
    val actressViewModel: ActressViewModel = viewModel()
    val netDiskViewModel: NetDiskViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val uiState by libraryViewModel.uiState.collectAsState()
    val actressState by actressViewModel.uiState.collectAsState()

    Scaffold(
        containerColor = colors.appBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            when (selectedTab) {
                MainTab.Home -> HomeSearchBar(
                    value = uiState.searchInput,
                    onValueChange = libraryViewModel::onSearchInputChanged,
                )
                MainTab.Actress -> HomeSearchBar(
                    value = actressState.searchInput,
                    onValueChange = actressViewModel::onSearchInputChanged,
                    placeholder = PLACEHOLDER_ACTRESS,
                )
                MainTab.NetDisk -> SimpleTopBar(title = TAB_NET_DISK)
                MainTab.Settings -> SimpleTopBar(title = TAB_SETTINGS)
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(88.dp),
                containerColor = colors.topBar,
                contentColor = colors.textPrimary,
            ) {
                NavigationBarItem(
                    selected = selectedTab == MainTab.Home,
                    onClick = { selectedTab = MainTab.Home },
                    icon = { Icon(Icons.Rounded.Home, contentDescription = TAB_HOME) },
                    label = { Text(TAB_HOME) },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Actress,
                    onClick = { selectedTab = MainTab.Actress },
                    icon = { Icon(Icons.Rounded.Person, contentDescription = TAB_ACTRESS) },
                    label = { Text(TAB_ACTRESS) },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.NetDisk,
                    onClick = { selectedTab = MainTab.NetDisk },
                    icon = { Icon(Icons.Rounded.Folder, contentDescription = TAB_NET_DISK) },
                    label = { Text(TAB_NET_DISK) },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Settings,
                    onClick = { selectedTab = MainTab.Settings },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = TAB_SETTINGS) },
                    label = { Text(TAB_SETTINGS) },
                )
            }
        },
    ) { innerPadding ->
        val screenPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding(),
        )
        when (selectedTab) {
            MainTab.Home -> LibraryScreen(
                onOpenPlayer = onOpenPlayer,
                onOpenDetail = onOpenDetail,
                contentPadding = screenPadding,
                viewModel = libraryViewModel,
            )
            MainTab.Actress -> ActressScreen(
                contentPadding = screenPadding,
                viewModel = actressViewModel,
            )
            MainTab.NetDisk -> NetDiskScreen(
                contentPadding = screenPadding,
                viewModel = netDiskViewModel,
            )
            MainTab.Settings -> SettingsScreen(
                contentPadding = screenPadding,
                viewModel = settingsViewModel,
            )
        }
    }
}

@Composable
private fun HomeSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = PLACEHOLDER_MOVIE,
) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.topBar)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceVariant,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxSize(),
                singleLine = true,
                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                    color = colors.textPrimary,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                cursorBrush = SolidColor(colors.textPrimary),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = LABEL_SEARCH,
                            modifier = Modifier.size(20.dp),
                            tint = colors.textTertiary,
                        )
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = colors.textTertiary,
                                )
                            }
                            innerTextField()
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun SimpleTopBar(title: String) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.topBar)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = colors.textPrimary)
    }
}

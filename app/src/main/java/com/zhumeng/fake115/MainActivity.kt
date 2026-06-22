package com.zhumeng.fake115

import android.os.Bundle
import android.view.WindowManager
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhumeng.fake115.data.model.LibraryMovie
import com.zhumeng.fake115.ui.actress.ActressScreen
import com.zhumeng.fake115.ui.actress.ActressViewModel
import com.zhumeng.fake115.ui.home.LibraryScreen
import com.zhumeng.fake115.ui.home.LibraryDetailFilterType
import com.zhumeng.fake115.ui.home.LibraryViewModel
import com.zhumeng.fake115.ui.netdisk.NetDiskScreen
import com.zhumeng.fake115.ui.netdisk.NetDiskViewModel
import com.zhumeng.fake115.ui.settings.SettingsScreen
import com.zhumeng.fake115.ui.settings.SettingsViewModel
import com.zhumeng.fake115.ui.theme.AppTheme
import com.zhumeng.fake115.ui.theme.Fake115Theme

private const val TAB_HOME = "首页"
private const val TAB_ACTRESS = "演员"
private const val TAB_NET_DISK = "网盘"
private const val TAB_SETTINGS = "设置"
private const val PLACEHOLDER_MOVIE = "搜索标题或番号"
private const val PLACEHOLDER_ACTRESS = "搜索演员"
private const val PLACEHOLDER_NET_DISK = "搜索网盘文件"
private const val LABEL_SEARCH = "搜索"

class MainActivity : ComponentActivity() {
    private val targetNetDiskCid = mutableStateOf<String?>(null)
    private val targetLibraryFilter = mutableStateOf<LibraryFilterTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        targetNetDiskCid.value = intent.getStringExtra(EXTRA_NET_DISK_CID)
        targetLibraryFilter.value = intent.readLibraryFilterTarget()
        setContent {
            Fake115Theme {
                MainScreen(
                    targetNetDiskCid = targetNetDiskCid.value,
                    targetLibraryFilter = targetLibraryFilter.value,
                    onNetDiskTargetConsumed = { targetNetDiskCid.value = null },
                    onLibraryFilterTargetConsumed = { targetLibraryFilter.value = null },
                    onOpenPlayer = { movie, playlist ->
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
                                playlist = playlist,
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        targetNetDiskCid.value = intent.getStringExtra(EXTRA_NET_DISK_CID)
        targetLibraryFilter.value = intent.readLibraryFilterTarget()
    }

    companion object {
        private const val EXTRA_NET_DISK_CID = "extra_net_disk_cid"
        private const val EXTRA_LIBRARY_FILTER_KEY = "extra_library_filter_key"
        private const val EXTRA_LIBRARY_FILTER_VALUE = "extra_library_filter_value"

        fun createNetDiskIntent(
            context: Context,
            cid: String,
        ): Intent {
            return Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NET_DISK_CID, cid.ifBlank { "0" })
            }
        }

        fun createLibraryFilterIntent(
            context: Context,
            queryKey: String,
            value: String,
        ): Intent {
            return Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_LIBRARY_FILTER_KEY, queryKey)
                putExtra(EXTRA_LIBRARY_FILTER_VALUE, value)
            }
        }

        private fun Intent.readLibraryFilterTarget(): LibraryFilterTarget? {
            val type = getStringExtra(EXTRA_LIBRARY_FILTER_KEY)
                ?.let(LibraryDetailFilterType::fromQueryKey)
                ?: return null
            val value = getStringExtra(EXTRA_LIBRARY_FILTER_VALUE)
                ?.takeIf { it.isNotBlank() }
                ?: return null
            return LibraryFilterTarget(type, value)
        }
    }
}

private data class LibraryFilterTarget(
    val type: LibraryDetailFilterType,
    val value: String,
)

private enum class MainTab {
    Home,
    Actress,
    NetDisk,
    Settings,
}

private const val MAIN_PREFS_NAME = "main_prefs"
private const val KEY_SELECTED_TAB = "selected_tab"

@Composable
private fun MainScreen(
    targetNetDiskCid: String?,
    targetLibraryFilter: LibraryFilterTarget?,
    onNetDiskTargetConsumed: () -> Unit,
    onLibraryFilterTargetConsumed: () -> Unit,
    onOpenPlayer: (LibraryMovie, List<LibraryMovie>) -> Unit,
    onOpenDetail: (LibraryMovie) -> Unit,
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val mainPrefs = remember(context) {
        context.getSharedPreferences(MAIN_PREFS_NAME, Context.MODE_PRIVATE)
    }
    var selectedTab by rememberSaveable {
        mutableStateOf(
            MainTab.entries.firstOrNull {
                it.name == mainPrefs.getString(KEY_SELECTED_TAB, MainTab.NetDisk.name)
            } ?: MainTab.NetDisk
        )
    }
    val libraryViewModel: LibraryViewModel = viewModel()
    val actressViewModel: ActressViewModel = viewModel()
    val netDiskViewModel: NetDiskViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val uiState by libraryViewModel.uiState.collectAsState()
    val actressState by actressViewModel.uiState.collectAsState()
    val netDiskState by netDiskViewModel.uiState.collectAsState()
    val actressGridState = rememberLazyStaggeredGridState()
    var settingsTitle by rememberSaveable { mutableStateOf(TAB_SETTINGS) }
    var settingsCanNavigateBack by rememberSaveable { mutableStateOf(false) }
    var settingsBackRequestToken by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(targetNetDiskCid) {
        val cid = targetNetDiskCid ?: return@LaunchedEffect
        selectedTab = MainTab.NetDisk
        mainPrefs.edit().putString(KEY_SELECTED_TAB, MainTab.NetDisk.name).apply()
        netDiskViewModel.openPath(cid)
        onNetDiskTargetConsumed()
    }

    LaunchedEffect(targetLibraryFilter) {
        val target = targetLibraryFilter ?: return@LaunchedEffect
        selectedTab = MainTab.Home
        mainPrefs.edit().putString(KEY_SELECTED_TAB, MainTab.Home.name).apply()
        libraryViewModel.filterByDetailTag(target.type, target.value)
        onLibraryFilterTargetConsumed()
    }

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
                MainTab.NetDisk -> HomeSearchBar(
                    value = netDiskState.searchInput,
                    onValueChange = netDiskViewModel::onSearchInputChanged,
                    placeholder = PLACEHOLDER_NET_DISK,
                )
                MainTab.Settings -> SimpleTopBar(
                    title = settingsTitle,
                    onBack = if (settingsCanNavigateBack) {
                        { settingsBackRequestToken += 1 }
                    } else {
                        null
                    },
                )
            }
        },
        bottomBar = {
            val selectedTabColor = Color(0xFF4FB7FF)
            val unselectedTabColor = Color(0xFF8C96A8)
            NavigationBar(
                modifier = Modifier.height(80.dp),
                containerColor = colors.topBar,
                contentColor = selectedTabColor,
            ) {
                MainNavigationItem(
                    selected = selectedTab == MainTab.Home,
                    onClick = {
                        selectedTab = MainTab.Home
                        mainPrefs.edit().putString(KEY_SELECTED_TAB, MainTab.Home.name).apply()
                    },
                    icon = Icons.Rounded.Home,
                    label = TAB_HOME,
                    selectedColor = selectedTabColor,
                    unselectedColor = unselectedTabColor,
                )
                MainNavigationItem(
                    selected = selectedTab == MainTab.Actress,
                    onClick = {
                        selectedTab = MainTab.Actress
                        mainPrefs.edit().putString(KEY_SELECTED_TAB, MainTab.Actress.name).apply()
                    },
                    icon = Icons.Rounded.Person,
                    label = TAB_ACTRESS,
                    selectedColor = selectedTabColor,
                    unselectedColor = unselectedTabColor,
                )
                MainNavigationItem(
                    selected = selectedTab == MainTab.NetDisk,
                    onClick = {
                        selectedTab = MainTab.NetDisk
                        mainPrefs.edit().putString(KEY_SELECTED_TAB, MainTab.NetDisk.name).apply()
                    },
                    icon = Icons.Rounded.Folder,
                    label = TAB_NET_DISK,
                    selectedColor = selectedTabColor,
                    unselectedColor = unselectedTabColor,
                )
                MainNavigationItem(
                    selected = selectedTab == MainTab.Settings,
                    onClick = {
                        selectedTab = MainTab.Settings
                        mainPrefs.edit().putString(KEY_SELECTED_TAB, MainTab.Settings.name).apply()
                    },
                    icon = Icons.Rounded.Settings,
                    label = TAB_SETTINGS,
                    selectedColor = selectedTabColor,
                    unselectedColor = unselectedTabColor,
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
                gridState = actressGridState,
                onActressSelected = { actress ->
                    libraryViewModel.filterByActress(actress.name)
                    selectedTab = MainTab.Home
                    mainPrefs.edit().putString(KEY_SELECTED_TAB, MainTab.Home.name).apply()
                },
            )
            MainTab.NetDisk -> NetDiskScreen(
                contentPadding = screenPadding,
                viewModel = netDiskViewModel,
            )
            MainTab.Settings -> SettingsScreen(
                contentPadding = screenPadding,
                viewModel = settingsViewModel,
                onTitleChanged = { settingsTitle = it },
                onCanNavigateBackChanged = { settingsCanNavigateBack = it },
                backRequestToken = settingsBackRequestToken,
            )
        }
    }
}

@Composable
private fun RowScope.MainNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    selectedColor: Color,
    unselectedColor: Color,
) {
    val tint = if (selected) selectedColor else unselectedColor
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            color = tint,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
        )
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
private fun SimpleTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.topBar)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = colors.textPrimary,
                )
            }
        }
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

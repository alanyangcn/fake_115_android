package com.zhumeng.fake115.ui.actress

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.zhumeng.fake115.data.model.Actress
import com.zhumeng.fake115.data.model.FavoriteFilterMode
import com.zhumeng.fake115.ui.theme.AppTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ActressScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: ActressViewModel = viewModel(),
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onActressSelected: (Actress) -> Unit = {},
) {
    val colors = AppTheme.colors
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
    )

    LaunchedEffect(viewModel, context) {
        viewModel.toastMessages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(gridState, state.actresses.size, state.hasMore, state.isLoadingMore) {
        gridState.loadMoreWhenNeeded(
            itemCount = state.actresses.size,
            hasMore = state.hasMore,
            isLoadingMore = state.isLoadingMore,
            onLoadMore = viewModel::loadMore,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground)
            .pullRefresh(pullRefreshState),
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(3),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 10.dp,
                end = 10.dp,
                top = contentPadding.calculateTopPadding() + 58.dp,
                bottom = contentPadding.calculateBottomPadding() + 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp,
        ) {
            when {
                state.isLoading && state.actresses.isEmpty() -> {
                    items(12) { ActressLoadingCard() }
                }

                state.actresses.isEmpty() -> {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        ActressEmptyState(errorMessage = state.errorMessage)
                    }
                }

                else -> {
                    items(state.actresses, key = { it.id }) { actress ->
                        ActressCard(
                            actress = actress,
                            favoriteUpdating = actress.id in state.favoriteUpdatingIds,
                            videosFavoriteUpdating = actress.id in state.videosFavoriteUpdatingIds,
                            onClick = { onActressSelected(actress) },
                            onFavorite = { viewModel.toggleFavorite(actress.id) },
                            onToggleAllVideosFavorite = { viewModel.toggleAllVideosFavorite(actress.id) },
                        )
                    }

                    if (state.isLoadingMore) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }

        ActressToolbar(
            favoriteFilter = state.favoriteFilter,
            sortOrder = state.sortOrder,
            onToggleFavoriteFilter = viewModel::cycleFavoriteFilter,
            onToggleOrder = viewModel::toggleSortOrder,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(colors.topBar)
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    bottom = 8.dp,
                ),
        )

        PullRefreshIndicator(
            refreshing = state.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = contentPadding.calculateTopPadding() + 58.dp),
        )
    }
}

@Composable
private fun ActressToolbar(
    favoriteFilter: FavoriteFilterMode,
    sortOrder: String,
    onToggleFavoriteFilter: () -> Unit,
    onToggleOrder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val sortLabel = if (sortOrder == "desc") "作品数倒序" else "作品数正序"
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ToolbarIconButton(
            modifier = Modifier.weight(1f),
            icon = when (favoriteFilter) {
                FavoriteFilterMode.All -> Icons.Rounded.FavoriteBorder
                FavoriteFilterMode.Favorite -> Icons.Rounded.Favorite
                FavoriteFilterMode.Unfavorite -> Icons.Rounded.FavoriteBorder
            },
            contentDescription = when (favoriteFilter) {
                FavoriteFilterMode.All -> "显示全部演员"
                FavoriteFilterMode.Favorite -> "仅看已收藏演员"
                FavoriteFilterMode.Unfavorite -> "仅看未收藏演员"
            },
            onClick = onToggleFavoriteFilter,
            containerColor = when (favoriteFilter) {
                FavoriteFilterMode.All -> colors.surfaceVariant
                FavoriteFilterMode.Favorite -> colors.dangerSoft
                FavoriteFilterMode.Unfavorite -> colors.dangerSoft
            },
            contentColor = when (favoriteFilter) {
                FavoriteFilterMode.All -> colors.textPrimary
                FavoriteFilterMode.Favorite -> colors.danger
                FavoriteFilterMode.Unfavorite -> colors.danger
            },
        )
        ToolbarTextButton(
            modifier = Modifier.weight(2f),
            icon = Icons.Rounded.SwapVert,
            text = sortLabel,
            onClick = onToggleOrder,
            containerColor = if (sortOrder == "desc") colors.accentSoft else colors.surfaceVariant,
        )
    }
}

@Composable
private fun ToolbarIconButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
) {
    val colors = AppTheme.colors
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (containerColor == Color.Unspecified) colors.surfaceVariant else containerColor,
            contentColor = if (contentColor == Color.Unspecified) colors.textPrimary else contentColor,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ActressCard(
    actress: Actress,
    favoriteUpdating: Boolean,
    videosFavoriteUpdating: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onToggleAllVideosFavorite: () -> Unit,
) {
    val colors = AppTheme.colors
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopEnd,
            ) {
                if (!actress.avatar.isNullOrBlank()) {
                    AsyncImage(
                        model = actress.avatar,
                        contentDescription = actress.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(colors.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "暂无头像",
                            color = colors.textTertiary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, end = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accent.copy(alpha = 0.72f))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = actress.videoCount.toString(),
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text(
                text = actress.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 2.dp),
                color = colors.textPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ActressActionButton(
                    modifier = Modifier.weight(1f),
                    icon = if (actress.isFavorite == 1) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (actress.isFavorite == 1) "取消收藏演员" else "收藏演员",
                    onClick = onFavorite,
                    enabled = !favoriteUpdating,
                    loading = favoriteUpdating,
                    active = actress.isFavorite == 1,
                )
                ActressActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.VideoLibrary,
                    contentDescription = if (actress.isFavoriteAllVideos == 1) {
                        "取消收藏所有影片"
                    } else {
                        "收藏所有影片"
                    },
                    onClick = onToggleAllVideosFavorite,
                    enabled = !videosFavoriteUpdating,
                    loading = videosFavoriteUpdating,
                    active = actress.isFavoriteAllVideos == 1,
                )
            }
        }
    }
}

@Composable
private fun ToolbarTextButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    containerColor: Color = Color.Unspecified,
) {
    val colors = AppTheme.colors
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (containerColor == Color.Unspecified) colors.surfaceVariant else containerColor,
            contentColor = colors.textPrimary,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ActressActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    loading: Boolean,
    active: Boolean,
) {
    val colors = AppTheme.colors
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (active) colors.dangerSoft else colors.surfaceVariant,
            contentColor = if (active) colors.danger else colors.textPrimary,
            disabledContainerColor = if (active) colors.dangerSoft else colors.surfaceVariant,
            disabledContentColor = if (active) colors.danger else colors.textTertiary,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = if (active) colors.danger else colors.textPrimary,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ActressLoadingCard() {
    val colors = AppTheme.colors
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceVariant),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.72f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.placeholderSurface),
            )
        }
    }
}

@Composable
private fun ActressEmptyState(errorMessage: String?) {
    val colors = AppTheme.colors
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "没有找到演员",
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = errorMessage ?: "试试换个关键词重新搜索。",
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private suspend fun LazyStaggeredGridState.loadMoreWhenNeeded(
    itemCount: Int,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
) {
    snapshotFlow {
        layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    }.collect { lastVisible: Int ->
        val triggerIndex = itemCount - 9
        if (hasMore && !isLoadingMore && itemCount > 0 && lastVisible >= triggerIndex) {
            onLoadMore()
        }
    }
}

package com.zhumeng.fake115.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhumeng.fake115.ui.theme.AppTheme

private enum class SettingsPage {
    List,
    Cookie,
}

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: SettingsViewModel,
    onTitleChanged: (String) -> Unit = {},
    onCanNavigateBackChanged: (Boolean) -> Unit = {},
    backRequestToken: Int = 0,
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.List) }

    LaunchedEffect(page) {
        onTitleChanged(
            when (page) {
                SettingsPage.List -> "设置"
                SettingsPage.Cookie -> "115 Cookie"
            }
        )
        onCanNavigateBackChanged(page != SettingsPage.List)
    }

    LaunchedEffect(backRequestToken) {
        if (backRequestToken > 0 && page != SettingsPage.List) {
            page = SettingsPage.List
        }
    }

    BackHandler(enabled = page != SettingsPage.List) {
        page = SettingsPage.List
    }

    when (page) {
        SettingsPage.List -> SettingsListPage(
            contentPadding = contentPadding,
            viewModel = viewModel,
            onOpenCookie = { page = SettingsPage.Cookie },
        )
        SettingsPage.Cookie -> CookieSettingsPage(
            contentPadding = contentPadding,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun SettingsListPage(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel,
    onOpenCookie: () -> Unit,
) {
    val colors = AppTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground)
            .padding(contentPadding)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SettingsSectionLabel(text = "账号")
        SettingsRow(
            icon = Icons.Rounded.Cookie,
            title = "115 Cookie",
            subtitle = if (uiState.hasSavedCookie) "已保存本地 Cookie" else "未保存本地 Cookie",
            onClick = onOpenCookie,
            trailing = {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowRight,
                    contentDescription = "进入",
                    tint = colors.textTertiary,
                )
            },
        )

        SettingsSectionLabel(text = "操作")
        SettingsRow(
            icon = Icons.Rounded.FlashOn,
            title = "快捷管理",
            subtitle = "开启后删除文件不再二次确认",
            trailing = {
                Switch(
                    checked = uiState.quickManagementEnabled,
                    onCheckedChange = viewModel::setQuickManagementEnabled,
                )
            },
        )
    }
}

@Composable
private fun CookieSettingsPage(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel,
) {
    val colors = AppTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground)
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Cookie 内容",
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "可以手动输入，也可以从服务端获取后保存到本地。",
                    color = colors.textTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = uiState.cookieInput,
                    onValueChange = viewModel::onCookieChanged,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    maxLines = 10,
                    label = { Text("Cookie") },
                    placeholder = { Text("UID=...; CID=...; SEID=...") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = viewModel::fetchCookie,
                        enabled = !uiState.isFetching,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (uiState.isFetching) "获取中..." else "获取")
                    }
                    Button(
                        onClick = viewModel::saveCookie,
                        enabled = !uiState.isFetching,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("保存")
                    }
                }
                Text(
                    text = if (uiState.hasSavedCookie) {
                        "当前已保存本地 Cookie。"
                    } else {
                        "当前还未保存本地 Cookie。"
                    },
                    color = colors.textTertiary,
                )
                uiState.message?.let { message ->
                    Text(
                        text = message,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(
    text: String,
) {
    val colors = AppTheme.colors
    Text(
        text = text,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
        color = colors.textTertiary,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    val colors = AppTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(colors.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textPrimary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = colors.textTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trailing()
        }
    }
}

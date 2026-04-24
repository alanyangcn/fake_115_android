package com.zhumeng.fake115.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.zhumeng.fake115.ui.theme.AppTheme

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: SettingsViewModel,
) {
    val colors = AppTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground)
            .padding(contentPadding)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "115 Cookie",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "\u53ef\u4ee5\u624b\u52a8\u8f93\u5165 Cookie\uff0c\u4e5f\u53ef\u4ee5\u4ece\u670d\u52a1\u7aef\u83b7\u53d6\u540e\u518d\u4fdd\u5b58\u5230\u672c\u5730\u3002",
                    color = colors.textTertiary
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = viewModel::fetchCookie,
                        enabled = !uiState.isFetching,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (uiState.isFetching) "\u83b7\u53d6\u4e2d..." else "\u83b7\u53d6")
                    }
                    Button(
                        onClick = viewModel::saveCookie,
                        enabled = !uiState.isFetching,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("\u4fdd\u5b58")
                    }
                }
                Text(
                    text = if (uiState.hasSavedCookie) {
                        "\u5f53\u524d\u5df2\u4fdd\u5b58\u672c\u5730 Cookie\u3002"
                    } else {
                        "\u5f53\u524d\u8fd8\u672a\u4fdd\u5b58\u672c\u5730 Cookie\u3002"
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

package com.zhumeng.fake115.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun DeleteConfirmDialog(
    message: String,
    deleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String = "确认删除",
    confirmText: String = "确认删除",
    deletingText: String = "删除中...",
    dismissText: String = "取消",
    confirmEnabled: Boolean = !deleting,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
            ) {
                Text(if (deleting) deletingText else confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !deleting,
            ) {
                Text(dismissText)
            }
        },
    )
}

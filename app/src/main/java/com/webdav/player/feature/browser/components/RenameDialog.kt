package com.webdav.player.feature.browser.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * 重命名对话框
 *
 * 输入新文件名，确认/取消
 */
@Composable
fun RenameDialog(
    isVisible: Boolean,
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    var newName by remember(currentName) { mutableStateOf(currentName) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            focusRequester.requestFocus()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("新名称") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName.trim()) },
                enabled = newName.isNotBlank() && newName != currentName
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

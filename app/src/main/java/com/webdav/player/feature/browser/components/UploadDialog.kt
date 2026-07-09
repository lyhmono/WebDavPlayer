package com.webdav.player.feature.browser.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 上传对话框
 *
 * 使用系统文件选择器选择文件并上传
 * 显示上传进度，支持取消上传
 */
@Composable
fun UploadDialog(
    isVisible: Boolean,
    isUploading: Boolean,
    uploadProgress: Float,
    onFileSelected: (android.net.Uri) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onFileSelected(uri)
        }
    }

    if (isUploading) {
        // 上传进度对话框
        AlertDialog(
            onDismissRequest = { /* 不允许点击外部关闭 */ },
            title = { Text("上传中") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${(uploadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                OutlinedButton(onClick = onCancel) {
                    Text("取消上传")
                }
            }
        )
    } else {
        // 选择文件对话框
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("上传文件") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "选择要上传到当前目录的文件",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
                    Text("选择文件")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    }
}

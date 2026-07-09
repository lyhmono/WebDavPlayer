package com.webdav.player.feature.browser.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 多选操作栏组件
 *
 * 底部浮动操作栏，显示选中数量和操作按钮
 */
@Composable
fun SelectionActionBar(
    selectedCount: Int,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onSelectAll: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：取消 + 选中数量
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "取消",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "已选 $selectedCount 项",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // 右侧：操作按钮
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSelectAll) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = "全选",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onRename) {
                    Icon(
                        imageVector = Icons.Default.DriveFileRenameOutline,
                        contentDescription = "重命名",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onAddToPlaylist) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "加入播放列表",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

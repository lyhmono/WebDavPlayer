package com.webdav.player.feature.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.webdav.player.core.player.TrackInfo

/**
 * 字幕选择对话框
 *
 * 展示可用字幕列表，当前选中高亮
 * 支持"关闭字幕"选项
 * 预留"加载外部字幕"按钮（M5 先 Toast 提示）
 */
@Composable
fun SubtitleTrackDialog(
    subtitleTracks: List<TrackInfo>,
    onTrackSelected: (String) -> Unit,
    onDisableSubtitle: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isSubtitleDisabled = subtitleTracks.none { it.isSelected }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("字幕选择") },
        text = {
            Column {
                // "关闭字幕"选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSubtitleDisabled,
                            onClick = {
                                onDisableSubtitle()
                                onDismiss()
                            }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = isSubtitleDisabled,
                        onClick = null
                    )
                    Text(
                        text = "关闭字幕",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 字幕列表
                subtitleTracks.forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = track.isSelected,
                                onClick = {
                                    onTrackSelected(track.id)
                                    onDismiss()
                                }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = track.isSelected,
                            onClick = null
                        )
                        Column {
                            Text(
                                text = track.title ?: track.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (!track.language.isNullOrBlank() && track.language != track.title) {
                                Text(
                                    text = "语言: ${track.language}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (subtitleTracks.isEmpty()) {
                    Text(
                        text = "暂无内嵌字幕",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 加载外部字幕（M5 预留）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = false,
                            onClick = {
                                Toast.makeText(context, "功能开发中", Toast.LENGTH_SHORT).show()
                            }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = false,
                        onClick = null,
                        enabled = false
                    )
                    Text(
                        text = "加载外部字幕…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

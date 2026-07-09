package com.webdav.player.feature.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.webdav.player.core.player.TrackInfo

/**
 * 音轨选择对话框
 *
 * 展示可用音轨列表，当前选中高亮，支持"默认"选项
 */
@Composable
fun AudioTrackDialog(
    audioTracks: List<TrackInfo>,
    onTrackSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("音轨选择") },
        text = {
            Column {
                // "默认"选项（清除手动选择，让引擎自动选择）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = audioTracks.none { it.isSelected },
                            onClick = {
                                // 选择第一个音轨作为默认
                                val firstTrack = audioTracks.firstOrNull()
                                if (firstTrack != null) {
                                    onTrackSelected(firstTrack.id)
                                }
                                onDismiss()
                            }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = audioTracks.none { it.isSelected },
                        onClick = null
                    )
                    Text(
                        text = "默认",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // 音轨列表
                audioTracks.forEach { track ->
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

                if (audioTracks.isEmpty()) {
                    Text(
                        text = "暂无可用音轨",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
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

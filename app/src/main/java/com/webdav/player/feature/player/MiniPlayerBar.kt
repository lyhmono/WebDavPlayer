package com.webdav.player.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webdav.player.core.player.PlaybackState
import com.webdav.player.data.model.MediaType

/**
 * 迷你播放器条
 *
 * 底部浮动的迷你播放器，显示当前歌曲名、播放/暂停、下一首、进度条
 * 点击展开全屏播放页面
 */
@Composable
fun MiniPlayerBar(
    viewModel: PlayerViewModel,
    onClick: () -> Unit
) {
    val currentMedia by viewModel.currentMedia.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val queue by viewModel.queue.collectAsState()

    AnimatedVisibility(
        visible = queue.isNotEmpty() && currentMedia != null,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 媒体类型图标
                    val icon = if (currentMedia?.mediaType == MediaType.VIDEO) {
                        Icons.Default.VideoFile
                    } else {
                        Icons.Default.MusicNote
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // 标题
                    Text(
                        text = currentMedia?.displayName ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 播放/暂停
                    IconButton(onClick = {
                        if (playbackState is PlaybackState.Playing) {
                            viewModel.pause()
                        } else {
                            viewModel.play()
                        }
                    }) {
                        Icon(
                            imageVector = if (playbackState is PlaybackState.Playing) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = if (playbackState is PlaybackState.Playing) "暂停" else "播放",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 下一首
                    IconButton(onClick = { viewModel.next() }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "下一首",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 底部进度条
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

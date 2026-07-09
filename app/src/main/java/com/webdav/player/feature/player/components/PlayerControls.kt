package com.webdav.player.feature.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webdav.player.data.model.PlayMode
import com.webdav.player.feature.browser.util.Formatters
import kotlinx.coroutines.delay

/**
 * 播放器控制层
 *
 * 包含顶部栏（返回+标题）和底部控制栏（播放模式/上一首/播放暂停/下一首/播放列表）
 * 支持 3 秒无操作自动隐藏
 */
@Composable
fun PlayerControls(
    isVisible: Boolean,
    title: String,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    playMode: PlayMode,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlayModeClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onHideControls: () -> Unit
) {
    // 自动隐藏
    var autoHideTimer by remember { mutableStateOf(0) }
    LaunchedEffect(isVisible, autoHideTimer) {
        if (isVisible) {
            delay(3000)
            onHideControls()
        }
    }

    // 重置计时器
    LaunchedEffect(isPlaying, position) {
        if (isVisible) {
            autoHideTimer++
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            // 顶部栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // 底部控制栏
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // 进度条
                PlayerProgressSlider(
                    position = position,
                    duration = duration,
                    onSeek = onSeek
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 控制按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 播放模式
                    PlayModeButton(
                        playMode = playMode,
                        onClick = onPlayModeClick
                    )

                    // 上一首
                    IconButton(onClick = onPrevious) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "上一首",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // 播放/暂停
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // 下一首
                    IconButton(onClick = onNext) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "下一首",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // 播放列表
                    IconButton(onClick = onPlaylistClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "播放列表",
                            tint = Color.White
                        )
                    }
                }

                // 速度按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onSpeedClick) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "播放速度",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * 进度条组件
 */
@Composable
private fun PlayerProgressSlider(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    var sliderValue by remember(position) { mutableStateOf(position.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }

    Column {
        Slider(
            value = if (isDragging) sliderValue else position.toFloat(),
            onValueChange = {
                isDragging = true
                sliderValue = it
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek(sliderValue.toLong())
            },
            valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = Formatters.formatDuration(position),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Text(
                text = Formatters.formatDuration(duration),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
    }
}

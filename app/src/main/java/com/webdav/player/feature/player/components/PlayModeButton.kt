package com.webdav.player.feature.player.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.webdav.player.data.model.PlayMode

/**
 * 播放模式切换按钮
 *
 * 顺序 → 单曲循环 → 列表循环 → 随机，点击循环切换
 */
@Composable
fun PlayModeButton(
    playMode: PlayMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    val (icon, description) = when (playMode) {
        PlayMode.SEQUENCE -> Icons.Default.PlayArrow to "顺序播放"
        PlayMode.SINGLE_LOOP -> Icons.Default.RepeatOne to "单曲循环"
        PlayMode.LIST_LOOP -> Icons.Default.Repeat to "列表循环"
        PlayMode.SHUFFLE -> Icons.Default.Shuffle to "随机播放"
    }

    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint
        )
    }
}

/**
 * 获取播放模式对应的图标
 */
fun PlayMode.icon(): ImageVector = when (this) {
    PlayMode.SEQUENCE -> Icons.Default.PlayArrow
    PlayMode.SINGLE_LOOP -> Icons.Default.RepeatOne
    PlayMode.LIST_LOOP -> Icons.Default.Repeat
    PlayMode.SHUFFLE -> Icons.Default.Shuffle
}

/**
 * 获取播放模式显示名称
 */
fun PlayMode.displayName(): String = when (this) {
    PlayMode.SEQUENCE -> "顺序播放"
    PlayMode.SINGLE_LOOP -> "单曲循环"
    PlayMode.LIST_LOOP -> "列表循环"
    PlayMode.SHUFFLE -> "随机播放"
}

package com.webdav.player.feature.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.wrapContentSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import com.webdav.player.core.player.PlaybackState
import com.webdav.player.data.model.MediaType
import com.webdav.player.feature.player.components.PlayerControls
import com.webdav.player.feature.player.components.PlaylistBottomSheet
import com.webdav.player.feature.player.components.SpeedSelectorDialog
import com.webdav.player.feature.player.util.GestureHelper

/**
 * 全屏播放页面
 *
 * 支持视频和音频播放，控制层 3 秒自动隐藏
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val playbackState by viewModel.playbackState.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val playMode by viewModel.playMode.collectAsState()
    val currentMedia by viewModel.currentMedia.collectAsState()
    val currentSpeed by viewModel.currentSpeed.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val exoPlayer = remember { viewModel.getExoPlayer() }

    // 锁定横屏（视频时）
    DisposableEffect(currentMedia?.mediaType) {
        val activity = context as? Activity
        if (currentMedia?.mediaType == MediaType.VIDEO) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            GestureHelper.resetWindowBrightness(context)
        }
    }

    // 保持屏幕常亮
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 手势识别
    val gestureHelper = remember {
        GestureHelper(
            context = context,
            onSeekPreview = { deltaMs ->
                // 预览 seek（可选：显示 seek 预览 UI）
            },
            onSeekConfirm = { deltaMs ->
                val newPosition = (progress.position + deltaMs).coerceIn(0, progress.duration)
                viewModel.seekTo(newPosition)
            },
            onBrightnessChange = { brightness ->
                GestureHelper.setWindowBrightness(context, brightness)
            },
            onVolumeChange = { _, _ ->
                // 音量已在 GestureHelper 内设置
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                    }
                )
            }
    ) {
        // 视频播放区域 / 音频占位
        val isVideo = currentMedia?.mediaType == MediaType.VIDEO
        if (isVideo) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 音频模式：显示专辑封面占位
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.padding(64.dp),
                    tint = Color.White.copy(alpha = 0.5f)
                )
                currentMedia?.let {
                    Text(
                        text = it.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }

        // 控制层
        PlayerControls(
            isVisible = controlsVisible,
            title = currentMedia?.displayName ?: "",
            isPlaying = playbackState is PlaybackState.Playing,
            position = progress.position,
            duration = progress.duration,
            playMode = playMode,
            onBack = onBack,
            onPlayPause = {
                if (playbackState is PlaybackState.Playing) {
                    viewModel.pause()
                } else {
                    viewModel.play()
                }
            },
            onPrevious = { viewModel.previous() },
            onNext = { viewModel.next() },
            onSeek = { viewModel.seekTo(it) },
            onPlayModeClick = { viewModel.cyclePlayMode() },
            onSpeedClick = { showSpeedDialog = true },
            onPlaylistClick = { showPlaylistSheet = true },
            onHideControls = { controlsVisible = false }
        )
    }

    // 播放队列弹窗
    if (showPlaylistSheet) {
        PlaylistBottomSheet(
            queue = queue,
            currentIndex = currentIndex,
            onItemClick = { index ->
                viewModel.playAtIndex(index)
                showPlaylistSheet = false
            },
            onRemoveItem = { index -> viewModel.removeFromQueue(index) },
            onClearAll = { viewModel.clearQueue() },
            onDismiss = { showPlaylistSheet = false }
        )
    }

    // 速度选择对话框
    if (showSpeedDialog) {
        SpeedSelectorDialog(
            currentSpeed = currentSpeed,
            onSpeedSelected = { speed -> viewModel.setPlaybackSpeed(speed) },
            onDismiss = { showSpeedDialog = false }
        )
    }
}

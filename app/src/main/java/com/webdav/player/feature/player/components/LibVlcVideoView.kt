package com.webdav.player.feature.player.components

import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.webdav.player.core.player.libvlc.LibVlcPlayerEngine

/**
 * LibVLC 视频渲染组件
 *
 * 使用 AndroidView 封装 LibVLC 的 SurfaceView
 * 接收 LibVlcPlayerEngine，获取其 MediaPlayer，attach 到 SurfaceView
 */
@Composable
fun LibVlcVideoView(
    engine: LibVlcPlayerEngine,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).also { surfaceView ->
                engine.attachSurfaceView(surfaceView)
            }
        },
        modifier = modifier
    )

    // 组件销毁时解绑 SurfaceView
    DisposableEffect(Unit) {
        onDispose {
            engine.detachSurfaceView()
        }
    }
}

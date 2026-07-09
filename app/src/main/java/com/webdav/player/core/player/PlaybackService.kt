package com.webdav.player.core.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.webdav.player.MainActivity
import com.webdav.player.core.player.exoplayer.ExoPlayerEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 播放服务
 *
 * 基于 Media3 MediaSessionService，管理前台播放、通知栏控制、锁屏控制
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var exoPlayerEngine: ExoPlayerEngine

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = exoPlayerEngine.getExoPlayer()

        // 创建启动 Activity 的 PendingIntent
        val sessionActivityIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 构建 MediaSession
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            // 不在此释放 player，由 ExoPlayerEngine 单例管理
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}

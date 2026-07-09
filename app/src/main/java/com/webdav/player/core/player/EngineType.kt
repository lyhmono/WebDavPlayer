package com.webdav.player.core.player

/**
 * 播放内核类型
 */
enum class EngineType {
    /** ExoPlayer (Media3) */
    EXOPLAYER,

    /** LibVLC (VLC for Android) */
    LIBVLC,

    /** 系统 MediaPlayer */
    SYSTEM
}

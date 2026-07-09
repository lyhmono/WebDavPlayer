package com.webdav.player.core.player

/**
 * 播放状态
 */
sealed class PlaybackState {
    /** 空闲 */
    data object Idle : PlaybackState()

    /** 缓冲中 */
    data object Buffering : PlaybackState()

    /** 就绪 */
    data object Ready : PlaybackState()

    /** 正在播放 */
    data object Playing : PlaybackState()

    /** 暂停 */
    data object Paused : PlaybackState()

    /** 已结束 */
    data object Ended : PlaybackState()

    /** 错误 */
    data class Error(val message: String, val cause: Throwable? = null) : PlaybackState()
}

/**
 * 播放器状态简化判断
 */
val PlaybackState.isPlaying: Boolean
    get() = this is PlaybackState.Playing

val PlaybackState.isBuffering: Boolean
    get() = this is PlaybackState.Buffering

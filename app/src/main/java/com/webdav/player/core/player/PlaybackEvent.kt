package com.webdav.player.core.player

/**
 * 播放事件
 */
sealed class PlaybackEvent {
    /** 状态变更 */
    data class StateChanged(val state: PlaybackState) : PlaybackEvent()

    /** 进度更新 */
    data class ProgressChanged(
        val position: Long,
        val duration: Long
    ) : PlaybackEvent()

    /** 缓冲进度 */
    data class BufferingChanged(val bufferedPercentage: Int) : PlaybackEvent()

    /** 播放错误 */
    data class Error(val message: String, val cause: Throwable? = null) : PlaybackEvent()

    /** 播放列表完成 */
    data object PlaylistEnded : PlaybackEvent()

    /** 跳转到下一首 */
    data object SkipToNext : PlaybackEvent()

    /** 跳转到上一首 */
    data object SkipToPrevious : PlaybackEvent()
}

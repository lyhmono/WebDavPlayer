package com.webdav.player.core.player

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 统一播放器引擎接口
 *
 * 抽象不同播放内核（ExoPlayer / LibVLC / MediaPlayer）的公共操作
 */
interface PlayerEngine {

    /** 当前内核类型 */
    val engineType: EngineType

    /** 播放状态流 */
    val state: Flow<PlaybackState>

    /** 播放事件流 */
    val events: Flow<PlaybackEvent>

    /** 当前位置 (毫秒) */
    val currentPosition: Long

    /** 总时长 (毫秒) */
    val duration: Long

    /** 是否正在播放 */
    val isPlaying: Boolean

    /**
     * 设置播放列表
     */
    fun setMediaItems(items: List<MediaSource>, startIndex: Int = 0)

    /**
     * 添加单个媒体项
     */
    fun addMediaItem(item: MediaSource)

    /**
     * 添加媒体项列表
     */
    fun addMediaItems(items: List<MediaSource>)

    /**
     * 移除指定位置的媒体项
     */
    fun removeMediaItem(index: Int)

    /**
     * 清空播放列表
     */
    fun clearMediaItems()

    /**
     * 准备播放
     */
    fun prepare()

    /**
     * 播放
     */
    fun play()

    /**
     * 暂停
     */
    fun pause()

    /**
     * 停止
     */
    fun stop()

    /**
     * 跳转到指定位置
     */
    fun seekTo(position: Long)

    /**
     * 跳转到指定媒体项
     */
    fun seekToItem(index: Int)

    /**
     * 下一首
     */
    fun next()

    /**
     * 上一首
     */
    fun previous()

    /**
     * 设置播放速度
     * @param speed 速度倍率，1.0 为正常速度
     */
    fun setPlaybackSpeed(speed: Float)

    /**
     * 设置音量
     * @param volume 0.0 ~ 1.0
     */
    fun setVolume(volume: Float)

    /**
     * 释放资源
     */
    fun release()

    // ============ M5: 音轨/字幕支持 ============

    /** 可用音轨列表 */
    val audioTracks: StateFlow<List<TrackInfo>>

    /** 可用字幕轨列表 */
    val subtitleTracks: StateFlow<List<TrackInfo>>

    /**
     * 获取可用音轨列表
     */
    fun getAudioTracks(): List<TrackInfo>

    /**
     * 获取可用字幕轨列表
     */
    fun getSubtitleTracks(): List<TrackInfo>

    /**
     * 选择音轨
     * @param trackId 轨道 ID
     */
    fun selectAudioTrack(trackId: String)

    /**
     * 选择字幕轨
     * @param trackId 轨道 ID
     */
    fun selectSubtitleTrack(trackId: String)

    /**
     * 关闭字幕
     */
    fun disableSubtitle()
}

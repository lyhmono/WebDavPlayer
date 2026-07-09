package com.webdav.player.feature.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webdav.player.core.player.EngineManager
import com.webdav.player.core.player.EngineType
import com.webdav.player.core.player.MediaSource
import com.webdav.player.core.player.PlaybackEvent
import com.webdav.player.core.player.PlaybackState
import com.webdav.player.core.player.PlayerEngine
import com.webdav.player.core.player.TrackInfo
import com.webdav.player.core.player.exoplayer.ExoPlayerEngine
import com.webdav.player.core.player.libvlc.LibVlcPlayerEngine
import com.webdav.player.data.model.PlayMode
import com.webdav.player.data.model.Playlist
import com.webdav.player.data.model.PlaylistItem
import com.webdav.player.data.model.WebDavEntry
import com.webdav.player.data.repository.PlaylistRepository
import com.webdav.player.data.repository.ServerConfigRepository
import com.webdav.player.data.repository.WebDavRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 播放器 ViewModel
 *
 * M5: 通过 EngineManager 管理播放引擎，支持引擎动态切换
 * 暴露音轨/字幕 StateFlow，支持音轨/字幕切换
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engineManager: EngineManager,
    private val playlistRepository: PlaylistRepository,
    private val serverConfigRepository: ServerConfigRepository,
    private val webDavRepository: WebDavRepository
) : ViewModel() {

    /** 当前引擎 */
    private val currentEngine: PlayerEngine get() = engineManager.getCurrentEngine()

    /** 播放状态 */
    val playbackState: StateFlow<PlaybackState> = currentEngine.state

    /** 当前队列（从 ExoPlayer 或 LibVLC 引擎获取） */
    val queue: StateFlow<List<MediaSource>>
        get() = when (currentEngine) {
            is ExoPlayerEngine -> (currentEngine as ExoPlayerEngine).queue
            is LibVlcPlayerEngine -> (currentEngine as LibVlcPlayerEngine).queue
            else -> MutableStateFlow(emptyList())
        }

    /** 当前播放索引 */
    val currentIndex: StateFlow<Int>
        get() = when (currentEngine) {
            is ExoPlayerEngine -> (currentEngine as ExoPlayerEngine).currentIndex
            is LibVlcPlayerEngine -> (currentEngine as LibVlcPlayerEngine).currentIndex
            else -> MutableStateFlow(-1)
        }

    /** 播放模式 */
    val playMode: StateFlow<PlayMode>
        get() = when (currentEngine) {
            is ExoPlayerEngine -> (currentEngine as ExoPlayerEngine).playMode
            is LibVlcPlayerEngine -> (currentEngine as LibVlcPlayerEngine).playMode
            else -> MutableStateFlow(PlayMode.SEQUENCE)
        }

    /** 音轨列表 */
    val audioTracks: StateFlow<List<TrackInfo>> = currentEngine.audioTracks

    /** 字幕列表 */
    val subtitleTracks: StateFlow<List<TrackInfo>> = currentEngine.subtitleTracks

    /** 当前引擎类型 */
    val currentEngineType: StateFlow<EngineType> = engineManager.currentEngineType

    /** 进度信息 */
    private val _progress = MutableStateFlow(ProgressInfo(0L, 0L))
    val progress: StateFlow<ProgressInfo> = _progress.asStateFlow()

    /** 当前播放的媒体信息 */
    private val _currentMedia = MutableStateFlow<MediaSource?>(null)
    val currentMedia: StateFlow<MediaSource?> = _currentMedia.asStateFlow()

    /** 是否有播放内容 */
    val hasMedia: Boolean get() = queue.value.isNotEmpty()

    /** 速度列表 */
    val availableSpeeds = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    /** 当前速度 */
    private val _currentSpeed = MutableStateFlow(1.0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed.asStateFlow()

    /** 进度跟踪作业 */
    private var progressJob: Job? = null

    init {
        // 监听播放事件
        viewModelScope.launch {
            currentEngine.events.collect { event ->
                when (event) {
                    is PlaybackEvent.ProgressChanged -> {
                        _progress.value = ProgressInfo(event.position, event.duration)
                    }
                    is PlaybackEvent.StateChanged -> {
                        if (event.state is PlaybackState.Playing) {
                            startProgressTracking()
                        } else {
                            stopProgressTracking()
                        }
                    }
                    else -> {}
                }
            }
        }

        // 监听索引变化，更新当前媒体
        viewModelScope.launch {
            currentIndex.collect { index ->
                _currentMedia.value = if (index >= 0 && index < queue.value.size) {
                    queue.value[index]
                } else {
                    null
                }
            }
        }
    }

    // ============ 播放控制 ============

    fun play() = currentEngine.play()
    fun pause() = currentEngine.pause()
    fun stop() = currentEngine.stop()
    fun next() = currentEngine.next()
    fun previous() = currentEngine.previous()

    fun seekTo(position: Long) {
        currentEngine.seekTo(position)
        _progress.update { it.copy(position = position) }
    }

    // ============ 速度控制 ============

    fun setPlaybackSpeed(speed: Float) {
        currentEngine.setPlaybackSpeed(speed)
        _currentSpeed.value = speed
    }

    // ============ 播放模式 ============

    fun cyclePlayMode() {
        when (currentEngine) {
            is ExoPlayerEngine -> currentEngine.cyclePlayMode()
            is LibVlcPlayerEngine -> currentEngine.cyclePlayMode()
        }
    }

    // ============ 队列管理 ============

    /**
     * 播放单个文件（替换队列）
     */
    fun playFile(mediaSource: MediaSource) {
        currentEngine.setMediaItems(listOf(mediaSource), 0)
        currentEngine.play()
    }

    /**
     * 播放文件列表
     */
    fun playFiles(mediaSources: List<MediaSource>, startIndex: Int = 0) {
        if (mediaSources.isEmpty()) return
        currentEngine.setMediaItems(mediaSources, startIndex)
        currentEngine.play()
    }

    /**
     * 添加到队列末尾
     */
    fun addToQueue(mediaSource: MediaSource) {
        currentEngine.addMediaItem(mediaSource)
    }

    /**
     * 添加多个到队列
     */
    fun addToQueue(mediaSources: List<MediaSource>) {
        currentEngine.addMediaItems(mediaSources)
    }

    /**
     * 从队列中移除
     */
    fun removeFromQueue(index: Int) {
        currentEngine.removeMediaItem(index)
    }

    /**
     * 清空队列
     */
    fun clearQueue() {
        currentEngine.clearMediaItems()
    }

    /**
     * 跳转到队列中的指定项
     */
    fun playAtIndex(index: Int) {
        currentEngine.seekToItem(index)
        currentEngine.play()
    }

    // ============ WebDavEntry → MediaSource 转换 ============

    /**
     * 将 WebDavEntry 转换为 MediaSource
     */
    fun webDavEntryToMediaSource(entry: WebDavEntry, serverId: Long): MediaSource {
        val mediaType = when {
            entry.isVideo -> com.webdav.player.data.model.MediaType.VIDEO
            entry.isAudio -> com.webdav.player.data.model.MediaType.AUDIO
            else -> com.webdav.player.data.model.MediaType.UNKNOWN
        }
        return MediaSource(
            id = "${serverId}_${entry.path}",
            serverId = serverId.toString(),
            path = entry.path,
            displayName = entry.displayName,
            mediaType = mediaType,
            size = entry.size,
            mimeType = entry.contentType.ifBlank { null }
        )
    }

    /**
     * 播放单个 WebDavEntry
     */
    fun playWebDavEntry(entry: WebDavEntry, serverId: Long) {
        val mediaSource = webDavEntryToMediaSource(entry, serverId)
        playFile(mediaSource)
    }

    /**
     * 播放当前目录所有可播放文件
     */
    fun playAllWebDavEntries(entries: List<WebDavEntry>, serverId: Long, startIndex: Int = 0) {
        val playable = entries.filter { it.isPlayable }
        if (playable.isEmpty()) return
        val mediaSources = playable.map { webDavEntryToMediaSource(it, serverId) }
        playFiles(mediaSources, startIndex)
    }

    // ============ 播放列表 ============

    /**
     * 从数据库播放列表加载到队列
     */
    fun playPlaylist(playlist: Playlist) {
        val mediaSources = playlist.items.map { item ->
            MediaSource(
                id = "${item.serverId}_${item.path}",
                serverId = item.serverId.toString(),
                path = item.path,
                displayName = item.displayName,
                mediaType = item.mediaType,
                size = item.size,
                mimeType = item.mimeType
            )
        }
        playFiles(mediaSources)
    }

    /**
     * 保存当前队列为播放列表
     */
    suspend fun saveQueueAsPlaylist(name: String): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val playlistId = playlistRepository.createPlaylist(name)
                val currentQueue = queue.value
                currentQueue.forEachIndexed { index, media ->
                    val item = PlaylistItem(
                        playlistId = playlistId,
                        serverId = media.serverId.toLongOrNull() ?: 0L,
                        path = media.path,
                        displayName = media.displayName,
                        mediaType = media.mediaType,
                        size = media.size,
                        mimeType = media.mimeType,
                        sortOrder = index
                    )
                    playlistRepository.addPlaylistItem(playlistId, item)
                }
                Result.success(playlistId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ============ M5: 音轨/字幕控制 ============

    fun selectAudioTrack(trackId: String) {
        currentEngine.selectAudioTrack(trackId)
    }

    fun selectSubtitleTrack(trackId: String) {
        currentEngine.selectSubtitleTrack(trackId)
    }

    fun disableSubtitle() {
        currentEngine.disableSubtitle()
    }

    // ============ M5: 引擎切换 ============

    /**
     * 切换播放引擎
     */
    fun switchEngine(type: EngineType) {
        engineManager.switchEngine(type)
    }

    // ============ 进度跟踪 ============

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                _progress.value = ProgressInfo(
                    position = currentEngine.currentPosition,
                    duration = currentEngine.duration
                )
                delay(500)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    /**
     * 获取底层 ExoPlayer 实例（供 UI 使用，仅当当前引擎为 ExoPlayer 时可用）
     */
    fun getExoPlayer() = engineManager.getExoPlayerEngine()?.getExoPlayer()

    /**
     * 获取 LibVLC 引擎实例（供 UI 渲染使用，仅当当前引擎为 LibVLC 时可用）
     */
    fun getLibVlcEngine(): LibVlcPlayerEngine? {
        return if (currentEngineType.value == EngineType.LIBVLC) {
            engineManager.getLibVlcEngine()
        } else {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressTracking()
    }
}

/**
 * 进度信息
 */
data class ProgressInfo(
    val position: Long,
    val duration: Long
) {
    val progress: Float
        get() = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
}

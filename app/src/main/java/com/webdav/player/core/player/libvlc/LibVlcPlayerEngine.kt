package com.webdav.player.core.player.libvlc

import android.content.Context
import android.view.SurfaceView
import com.webdav.player.core.player.EngineType
import com.webdav.player.core.player.MediaSource
import com.webdav.player.core.player.PlaybackEvent
import com.webdav.player.core.player.PlaybackState
import com.webdav.player.core.player.PlayerEngine
import com.webdav.player.core.player.TrackInfo
import com.webdav.player.core.player.TrackType
import com.webdav.player.data.model.PlayMode
import com.webdav.player.data.repository.ServerConfigRepository
import com.webdav.player.data.repository.WebDavRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LibVLC 播放引擎实现
 *
 * 基于 VLC for Android (LibVLC)，通过 HTTP 直接流式播放 WebDAV 文件
 * 支持音轨/字幕切换、播放速度、音量控制、播放模式等
 */
@Singleton
class LibVlcPlayerEngine @Inject constructor(
    private val context: Context,
    private val webDavRepository: WebDavRepository,
    private val serverConfigRepository: ServerConfigRepository
) : PlayerEngine {

    override val engineType: EngineType = EngineType.LIBVLC

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** LibVLC 实例 */
    private val libVLC: LibVLC by lazy {
        LibVLC(context, arrayListOf(
            "--network-caching=3000",
            "--hwdec=auto",
            "--no-drop-late-frames",
            "--no-skip-frames"
        ))
    }

    /** MediaPlayer 实例 */
    private val mediaPlayer: MediaPlayer by lazy {
        MediaPlayer(libVLC).apply {
            setEventListener(::onVlcEvent)
        }
    }

    /** 媒体工厂 */
    private val mediaFactory: LibVlcMediaFactory by lazy {
        LibVlcMediaFactory(context, serverConfigRepository, webDavRepository)
    }

    /** 播放状态流 */
    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /** 播放事件流 */
    private val _events = MutableSharedFlow<PlaybackEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<PlaybackEvent> = _events.asSharedFlow()

    /** 当前播放列表 */
    private val _queue = MutableStateFlow<List<MediaSource>>(emptyList())
    val queue: StateFlow<List<MediaSource>> = _queue.asStateFlow()

    /** 当前播放索引 */
    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    /** 播放模式 */
    private val _playMode = MutableStateFlow(PlayMode.SEQUENCE)
    val playMode: StateFlow<PlayMode> = _playMode.asStateFlow()

    /** 可用音轨列表 */
    private val _audioTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    override val audioTracks: StateFlow<List<TrackInfo>> = _audioTracks.asStateFlow()

    /** 可用字幕轨列表 */
    private val _subtitleTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    override val subtitleTracks: StateFlow<List<TrackInfo>> = _subtitleTracks.asStateFlow()

    /** 进度更新作业 */
    private var progressJob: Job? = null

    /** 当前播放速度 */
    private var currentSpeed: Float = 1.0f

    /** 当前音量 */
    private var currentVolume: Float = 1.0f

    /** SurfaceView 引用 */
    private var surfaceView: SurfaceView? = null

    // ============ 队列管理 ============

    override fun setMediaItems(items: List<MediaSource>, startIndex: Int) {
        _queue.value = items
        _currentIndex.value = startIndex.coerceAtLeast(0)
        playAtIndexInternal(startIndex.coerceAtLeast(0))
    }

    override fun addMediaItem(item: MediaSource) {
        _queue.value = _queue.value + item
    }

    override fun addMediaItems(items: List<MediaSource>) {
        _queue.value = _queue.value + items
    }

    override fun removeMediaItem(index: Int) {
        if (index < 0 || index >= _queue.value.size) return
        _queue.value = _queue.value.toMutableList().apply { removeAt(index) }
        if (_currentIndex.value == index) {
            if (_queue.value.isNotEmpty()) {
                val newIndex = index.coerceAtMost(_queue.value.size - 1)
                _currentIndex.value = newIndex
                playAtIndexInternal(newIndex)
            } else {
                _currentIndex.value = -1
                stop()
            }
        } else if (_currentIndex.value > index) {
            _currentIndex.value = _currentIndex.value - 1
        }
    }

    override fun clearMediaItems() {
        _queue.value = emptyList()
        _currentIndex.value = -1
        stop()
    }

    // ============ 播放控制 ============

    override fun prepare() {
        // LibVLC 不需要显式 prepare
    }

    override fun play() {
        if (_state.value is PlaybackState.Paused) {
            mediaPlayer.play()
            _state.value = PlaybackState.Playing
            _events.tryEmit(PlaybackEvent.StateChanged(PlaybackState.Playing))
            startProgressTracking()
        } else if (_state.value is PlaybackState.Idle || _state.value is PlaybackState.Ended) {
            val index = _currentIndex.value
            if (index >= 0 && index < _queue.value.size) {
                playAtIndexInternal(index)
            }
        } else {
            mediaPlayer.play()
            _state.value = PlaybackState.Playing
            _events.tryEmit(PlaybackEvent.StateChanged(PlaybackState.Playing))
            startProgressTracking()
        }
    }

    override fun pause() {
        mediaPlayer.pause()
        _state.value = PlaybackState.Paused
        _events.tryEmit(PlaybackEvent.StateChanged(PlaybackState.Paused))
        stopProgressTracking()
    }

    override fun stop() {
        mediaPlayer.stop()
        _state.value = PlaybackState.Idle
        _events.tryEmit(PlaybackEvent.StateChanged(PlaybackState.Idle))
        stopProgressTracking()
    }

    override fun seekTo(position: Long) {
        val timeSec = position / 1000.0
        mediaPlayer.time = timeSec
    }

    override fun seekToItem(index: Int) {
        if (index < 0 || index >= _queue.value.size) return
        _currentIndex.value = index
        playAtIndexInternal(index)
    }

    override fun next() {
        val queue = _queue.value
        if (queue.isEmpty()) return

        val current = _currentIndex.value
        val mode = _playMode.value
        val nextIndex = calculateNextIndex(current, queue.size, mode)
        if (nextIndex >= 0 && nextIndex < queue.size) {
            seekToItem(nextIndex)
            _events.tryEmit(PlaybackEvent.SkipToNext)
        }
    }

    override fun previous() {
        val queue = _queue.value
        if (queue.isEmpty()) return

        val current = _currentIndex.value
        val mode = _playMode.value
        val prevIndex = calculatePrevIndex(current, queue.size, mode)
        if (prevIndex >= 0 && prevIndex < queue.size) {
            seekToItem(prevIndex)
            _events.tryEmit(PlaybackEvent.SkipToPrevious)
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed
        mediaPlayer.rate = speed
    }

    override fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        // LibVLC 音量范围 0-100
        mediaPlayer.volume = (currentVolume * 100).toInt()
    }

    override fun release() {
        stopProgressTracking()
        mediaPlayer.release()
        libVLC.release()
        scope.cancel()
    }

    // ============ 播放模式 ============

    fun setPlayMode(mode: PlayMode) {
        _playMode.value = mode
    }

    fun cyclePlayMode(): PlayMode {
        val next = when (_playMode.value) {
            PlayMode.SEQUENCE -> PlayMode.SINGLE_LOOP
            PlayMode.SINGLE_LOOP -> PlayMode.LIST_LOOP
            PlayMode.LIST_LOOP -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.SEQUENCE
        }
        setPlayMode(next)
        return next
    }

    // ============ 属性 ============

    override val currentPosition: Long
        get() = (mediaPlayer.time * 1000).toLong()

    override val duration: Long
        get() = (mediaPlayer.length * 1000).toLong()

    override val isPlaying: Boolean
        get() = mediaPlayer.isPlaying

    /** 获取底层 MediaPlayer 实例（供 UI 渲染使用） */
    fun getMediaPlayer(): MediaPlayer = mediaPlayer

    /**
     * 设置 SurfaceView 用于视频渲染
     */
    fun attachSurfaceView(surface: SurfaceView) {
        surfaceView = surface
        mediaPlayer.vlcVout.setVideoView(surface)
        mediaPlayer.vlcVout.attachViews()
    }

    /**
     * 解绑 SurfaceView
     */
    fun detachSurfaceView() {
        mediaPlayer.vlcVout.detachViews()
        surfaceView = null
    }

    // ============ M5: 音轨/字幕支持 ============

    override fun getAudioTracks(): List<TrackInfo> = _audioTracks.value

    override fun getSubtitleTracks(): List<TrackInfo> = _subtitleTracks.value

    override fun selectAudioTrack(trackId: String) {
        val tracks = mediaPlayer.getTrackDescription(IMedia.TrackType.Audio)
        for (track in tracks) {
            if (track.id.toString() == trackId) {
                mediaPlayer.setAudioTrack(track.id)
                updateTrackInfo()
                return
            }
        }
    }

    override fun selectSubtitleTrack(trackId: String) {
        val tracks = mediaPlayer.getTrackDescription(IMedia.TrackType.Text)
        for (track in tracks) {
            if (track.id.toString() == trackId) {
                mediaPlayer.setSpuTrack(track.id)
                updateTrackInfo()
                return
            }
        }
    }

    override fun disableSubtitle() {
        mediaPlayer.setSpuTrack(-1)
        updateTrackInfo()
    }

    /**
     * 更新轨道信息
     */
    private fun updateTrackInfo() {
        val audioList = mutableListOf<TrackInfo>()
        val subtitleList = mutableListOf<TrackInfo>()

        // 获取音轨
        try {
            val audioTracks = mediaPlayer.getTrackDescription(IMedia.TrackType.Audio)
            val currentAudioTrack = mediaPlayer.audioTrack

            audioTracks.forEachIndexed { index, track ->
                val name = track.name ?: "音轨 ${index + 1}"
                val parts = name.split(" - ")
                val language = parts.getOrNull(0)
                val title = parts.getOrNull(1) ?: name

                audioList.add(
                    TrackInfo(
                        id = track.id.toString(),
                        language = language,
                        title = title,
                        trackType = TrackType.AUDIO,
                        isSelected = track.id == currentAudioTrack
                    )
                )
            }
        } catch (_: Exception) {
            // 获取轨道信息时可能无媒体
        }

        // 获取字幕轨
        try {
            val spuTracks = mediaPlayer.getTrackDescription(IMedia.TrackType.Text)
            val currentSpuTrack = mediaPlayer.spuTrack

            spuTracks.forEachIndexed { index, track ->
                val name = track.name ?: "字幕 ${index + 1}"
                val parts = name.split(" - ")
                val language = parts.getOrNull(0)
                val title = parts.getOrNull(1) ?: name

                subtitleList.add(
                    TrackInfo(
                        id = track.id.toString(),
                        language = language,
                        title = title,
                        trackType = TrackType.SUBTITLE,
                        isSelected = track.id == currentSpuTrack
                    )
                )
            }
        } catch (_: Exception) {
            // 获取轨道信息时可能无媒体
        }

        _audioTracks.value = audioList
        _subtitleTracks.value = subtitleList
    }

    // ============ 内部逻辑 ============

    /**
     * 播放指定索引的媒体
     */
    private fun playAtIndexInternal(index: Int) {
        val queue = _queue.value
        if (index < 0 || index >= queue.size) return

        _currentIndex.value = index

        val mediaSource = queue[index]
        val media = mediaFactory.createMedia(libVLC, mediaSource)

        mediaPlayer.media = media
        media.release()

        // 恢复速度和音量设置
        if (currentSpeed != 1.0f) {
            mediaPlayer.rate = currentSpeed
        }
        if (currentVolume != 1.0f) {
            mediaPlayer.volume = (currentVolume * 100).toInt()
        }

        mediaPlayer.play()
        _state.value = PlaybackState.Buffering
        _events.tryEmit(PlaybackEvent.StateChanged(PlaybackState.Buffering))

        // 延迟更新轨道信息（等待媒体加载）
        scope.launch {
            delay(1000)
            updateTrackInfo()
        }

        startProgressTracking()
    }

    /**
     * 处理播放结束
     */
    private fun handlePlaybackEnded() {
        val queue = _queue.value
        if (queue.isEmpty()) {
            _events.tryEmit(PlaybackEvent.PlaylistEnded)
            return
        }

        val current = _currentIndex.value
        val mode = _playMode.value

        when (mode) {
            PlayMode.SEQUENCE -> {
                if (current < queue.size - 1) {
                    next()
                } else {
                    _events.tryEmit(PlaybackEvent.PlaylistEnded)
                }
            }
            PlayMode.SINGLE_LOOP -> {
                seekToItem(current)
                play()
            }
            PlayMode.LIST_LOOP -> {
                next()
            }
            PlayMode.SHUFFLE -> {
                next()
            }
        }
    }

    /**
     * LibVLC 事件回调
     */
    private fun onVlcEvent(event: MediaPlayer.Event) {
        when (event.type) {
            MediaPlayer.Event.Playing -> {
                _state.value = PlaybackState.Playing
                _events.tryEmit(PlaybackEvent.StateChanged(PlaybackState.Playing))
                startProgressTracking()
                scope.launch { delay(500); updateTrackInfo() }
            }
            MediaPlayer.Event.Paused -> {
                _state.value = PlaybackState.Paused
                _events.tryEmit(PlaybackEvent.StateChanged(PlaybackState.Paused))
                stopProgressTracking()
            }
            MediaPlayer.Event.Stopped -> {
                _state.value = PlaybackState.Idle
                _events.tryEmit(PlaybackEvent.StateChanged(PlaybackState.Idle))
                stopProgressTracking()
            }
            MediaPlayer.Event.EndReached -> {
                _state.value = PlaybackState.Ended
                _events.tryEmit(PlaybackEvent.StateChanged(PlaybackState.Ended))
                stopProgressTracking()
                handlePlaybackEnded()
            }
            MediaPlayer.Event.EncounteredError -> {
                val state = PlaybackState.Error("LibVLC 播放错误")
                _state.value = state
                _events.tryEmit(PlaybackEvent.Error("LibVLC 播放错误"))
                stopProgressTracking()
            }
            MediaPlayer.Event.Buffering -> {
                if (event.buffering < 100f) {
                    if (_state.value !is PlaybackState.Playing) {
                        _state.value = PlaybackState.Buffering
                        _events.tryEmit(PlaybackEvent.StateChanged(PlaybackState.Buffering))
                    }
                    _events.tryEmit(PlaybackEvent.BufferingChanged(event.buffering.toInt()))
                }
            }
            MediaPlayer.Event.LengthChanged -> {
                // 媒体长度变化，更新轨道信息
                scope.launch { delay(300); updateTrackInfo() }
            }
            MediaPlayer.Event.ESAdded, MediaPlayer.Event.ESDeleted -> {
                // 轨道变化，更新轨道信息
                scope.launch { delay(300); updateTrackInfo() }
            }
        }
    }

    private fun calculateNextIndex(current: Int, size: Int, mode: PlayMode): Int {
        if (size == 0) return -1
        return when (mode) {
            PlayMode.SEQUENCE -> {
                if (current < size - 1) current + 1 else -1
            }
            PlayMode.SINGLE_LOOP -> current
            PlayMode.LIST_LOOP -> (current + 1) % size
            PlayMode.SHUFFLE -> {
                if (size == 1) 0
                else {
                    var next: Int
                    do {
                        next = (0 until size).random()
                    } while (next == current)
                    next
                }
            }
        }
    }

    private fun calculatePrevIndex(current: Int, size: Int, mode: PlayMode): Int {
        if (size == 0) return -1
        return when (mode) {
            PlayMode.SEQUENCE -> {
                if (current > 0) current - 1 else -1
            }
            PlayMode.SINGLE_LOOP -> current
            PlayMode.LIST_LOOP -> if (current > 0) current - 1 else size - 1
            PlayMode.SHUFFLE -> {
                if (size == 1) 0
                else {
                    var prev: Int
                    do {
                        prev = (0 until size).random()
                    } while (prev == current)
                    prev
                }
            }
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                val pos = currentPosition
                val dur = duration
                _events.tryEmit(PlaybackEvent.ProgressChanged(pos, dur))
                delay(500)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }
}

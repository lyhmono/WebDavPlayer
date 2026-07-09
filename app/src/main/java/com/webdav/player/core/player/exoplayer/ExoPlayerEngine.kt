package com.webdav.player.core.player.exoplayer

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.webdav.player.core.player.EngineType
import com.webdav.player.core.player.MediaSource as AppMediaSource
import com.webdav.player.core.player.PlaybackEvent
import com.webdav.player.core.player.PlaybackState
import com.webdav.player.core.player.PlayerEngine
import com.webdav.player.core.webdav.WebDavDataSourceFactory
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
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ExoPlayer 播放引擎实现
 *
 * 基于 Media3 ExoPlayer，通过 WebDavDataSourceFactory 读取 webdav:// URI
 */
@UnstableApi
@Singleton
class ExoPlayerEngine @Inject constructor(
    private val context: Context,
    private val webDavRepository: WebDavRepository,
    private val serverConfigRepository: ServerConfigRepository,
    private val okHttpClient: OkHttpClient
) : PlayerEngine {

    override val engineType: EngineType = EngineType.EXOPLAYER

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** WebDavDataSource 工厂，按 serverId 动态获取 WebDavClient */
    private val dataSourceFactory: WebDavDataSourceFactory by lazy {
        WebDavDataSourceFactory(
            webDavClientProvider = { serverId ->
                val config = serverConfigRepository.getById(serverId.toLong())
                    ?: throw IllegalStateException("服务器配置不存在: $serverId")
                webDavRepository.getClient(config)
            },
            okHttpClient = okHttpClient
        )
    }

    /** ExoPlayer 实例 */
    private val exoPlayer: ExoPlayer by lazy {
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    /** 播放状态流 */
    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /** 播放事件流 */
    private val _events = MutableSharedFlow<PlaybackEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<PlaybackEvent> = _events.asSharedFlow()

    /** 当前播放列表 */
    private val _queue = MutableStateFlow<List<AppMediaSource>>(emptyList())
    val queue: StateFlow<List<AppMediaSource>> = _queue.asStateFlow()

    /** 当前播放索引 */
    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    /** 播放模式 */
    private val _playMode = MutableStateFlow(PlayMode.SEQUENCE)
    val playMode: StateFlow<PlayMode> = _playMode.asStateFlow()

    /** 进度更新作业 */
    private var progressJob: Job? = null

    /** 监听器 */
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val newState = when (playbackState) {
                Player.STATE_IDLE -> PlaybackState.Idle
                Player.STATE_BUFFERING -> PlaybackState.Buffering
                Player.STATE_READY -> {
                    if (exoPlayer.playWhenReady) PlaybackState.Playing else PlaybackState.Paused
                }
                Player.STATE_ENDED -> {
                    handlePlaybackEnded()
                    PlaybackState.Ended
                }
                else -> PlaybackState.Idle
            }
            _state.value = newState
            _events.tryEmit(PlaybackEvent.StateChanged(newState))
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                _state.value = PlaybackState.Playing
                startProgressTracking()
            } else {
                if (_state.value !is PlaybackState.Ended && _state.value !is PlaybackState.Error) {
                    _state.value = PlaybackState.Paused
                }
                stopProgressTracking()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentIndex.value = exoPlayer.currentMediaItemIndex
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                _events.tryEmit(PlaybackEvent.SkipToNext)
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val state = PlaybackState.Error(error.message ?: "播放错误", error)
            _state.value = state
            _events.tryEmit(PlaybackEvent.Error(error.message ?: "播放错误", error))
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            // 速度变更回调
        }
    }

    init {
        exoPlayer.addListener(playerListener)
    }

    // ============ 队列管理 ============

    override fun setMediaItems(items: List<AppMediaSource>, startIndex: Int) {
        _queue.value = items
        val mediaItems = items.map { it.toMedia3Item() }
        exoPlayer.setMediaItems(mediaItems, startIndex.coerceAtLeast(0), 0L)
        _currentIndex.value = startIndex
        prepare()
    }

    override fun addMediaItem(item: AppMediaSource) {
        _queue.value = _queue.value + item
        exoPlayer.addMediaItem(item.toMedia3Item())
    }

    override fun addMediaItems(items: List<AppMediaSource>) {
        _queue.value = _queue.value + items
        exoPlayer.addMediaItems(items.map { it.toMedia3Item() })
    }

    override fun removeMediaItem(index: Int) {
        if (index < 0 || index >= _queue.value.size) return
        _queue.value = _queue.value.toMutableList().apply { removeAt(index) }
        exoPlayer.removeMediaItem(index)
    }

    override fun clearMediaItems() {
        _queue.value = emptyList()
        _currentIndex.value = -1
        exoPlayer.clearMediaItems()
    }

    // ============ 播放控制 ============

    override fun prepare() {
        exoPlayer.prepare()
    }

    override fun play() {
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun stop() {
        exoPlayer.stop()
        stopProgressTracking()
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    override fun seekToItem(index: Int) {
        if (index < 0 || index >= _queue.value.size) return
        exoPlayer.seekToDefaultPosition(index)
        _currentIndex.value = index
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
        exoPlayer.playbackParameters = PlaybackParameters(speed)
    }

    override fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
    }

    override fun release() {
        stopProgressTracking()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        scope.cancel()
    }

    // ============ 播放模式 ============

    fun setPlayMode(mode: PlayMode) {
        _playMode.value = mode
        when (mode) {
            PlayMode.SINGLE_LOOP -> exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            PlayMode.LIST_LOOP -> exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
            else -> exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
        }
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
        get() = if (exoPlayer.duration != C.TIME_UNSET) exoPlayer.currentPosition else 0L

    override val duration: Long
        get() = if (exoPlayer.duration != C.TIME_UNSET) exoPlayer.duration else 0L

    override val isPlaying: Boolean
        get() = exoPlayer.isPlaying

    /** 获取底层 ExoPlayer 实例（供 Service 使用） */
    fun getExoPlayer(): ExoPlayer = exoPlayer

    // ============ 内部逻辑 ============

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

    // ============ 转换 ============

    private fun AppMediaSource.toMedia3Item(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(displayName)
            .setArtist(serverId)
            .build()

        return MediaItem.Builder()
            .setUri(toUriString())
            .setMediaId(id)
            .setMediaMetadata(metadata)
            .build()
    }
}

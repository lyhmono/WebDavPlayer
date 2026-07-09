package com.webdav.player.core.player

import com.webdav.player.core.player.exoplayer.ExoPlayerEngine
import com.webdav.player.core.player.libvlc.LibVlcPlayerEngine
import com.webdav.player.data.model.PlayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 引擎管理器
 *
 * 管理当前活跃的播放引擎，支持 ExoPlayer ↔ LibVLC 动态切换
 * 切换时保存当前播放状态（位置/队列/模式/速度），切换后恢复
 */
@Singleton
class EngineManager @Inject constructor(
    private val exoPlayerEngine: ExoPlayerEngine,
    private val libVlcPlayerEngine: LibVlcPlayerEngine
) {
    /** 当前活跃引擎 */
    private val _currentEngine = MutableStateFlow<PlayerEngine>(exoPlayerEngine)
    val currentEngine: StateFlow<PlayerEngine> = _currentEngine.asStateFlow()

    /** 当前引擎类型 */
    private val _currentEngineType = MutableStateFlow(EngineType.EXOPLAYER)
    val currentEngineType: StateFlow<EngineType> = _currentEngineType.asStateFlow()

    /** 可用引擎列表（根据实际注入的依赖动态判断） */
    val availableEngines: List<EngineType> = listOf(EngineType.EXOPLAYER, EngineType.LIBVLC)

    /**
     * 获取当前引擎实例
     */
    fun getCurrentEngine(): PlayerEngine = _currentEngine.value

    /**
     * 切换引擎
     *
     * 保存当前播放状态 → 切换引擎 → 恢复播放状态
     *
     * @param type 目标引擎类型
     */
    fun switchEngine(type: EngineType) {
        if (type == _currentEngineType.value) return

        val fromEngine = _currentEngine.value
        val toEngine = when (type) {
            EngineType.EXOPLAYER -> exoPlayerEngine
            EngineType.LIBVLC -> libVlcPlayerEngine
            EngineType.SYSTEM -> return // 暂不支持 SYSTEM
        }

        // 1. 保存当前播放状态
        val savedState = saveEngineState(fromEngine)

        // 2. 停止当前引擎
        fromEngine.stop()

        // 3. 切换引擎
        _currentEngine.value = toEngine
        _currentEngineType.value = type

        // 4. 恢复播放状态到新引擎
        restoreEngineState(toEngine, savedState)
    }

    /**
     * 保存引擎播放状态
     */
    private fun saveEngineState(engine: PlayerEngine): SavedPlaybackState {
        val queue = when (engine) {
            is ExoPlayerEngine -> engine.queue.value
            is LibVlcPlayerEngine -> engine.queue.value
            else -> emptyList()
        }

        val currentIndex = when (engine) {
            is ExoPlayerEngine -> engine.currentIndex.value
            is LibVlcPlayerEngine -> engine.currentIndex.value
            else -> -1
        }

        val playMode = when (engine) {
            is ExoPlayerEngine -> engine.playMode.value
            is LibVlcPlayerEngine -> engine.playMode.value
            else -> PlayMode.SEQUENCE
        }

        val position = engine.currentPosition
        val isPlaying = engine.isPlaying

        return SavedPlaybackState(
            queue = queue,
            currentIndex = currentIndex,
            position = position,
            playMode = playMode,
            isPlaying = isPlaying
        )
    }

    /**
     * 恢复引擎播放状态
     */
    private fun restoreEngineState(engine: PlayerEngine, state: SavedPlaybackState) {
        if (state.queue.isEmpty()) return

        // 恢复队列
        engine.setMediaItems(state.queue, state.currentIndex.coerceAtLeast(0))

        // 恢复播放模式
        when (engine) {
            is ExoPlayerEngine -> engine.setPlayMode(state.playMode)
            is LibVlcPlayerEngine -> engine.setPlayMode(state.playMode)
        }

        // 恢复播放位置
        if (state.position > 0) {
            engine.seekTo(state.position)
        }

        // 恢复播放状态
        if (state.isPlaying) {
            engine.play()
        }
    }

    /**
     * 获取 ExoPlayer 引擎实例（如果当前是 ExoPlayer）
     */
    fun getExoPlayerEngine(): ExoPlayerEngine? = exoPlayerEngine

    /**
     * 获取 LibVLC 引擎实例（如果当前是 LibVLC）
     */
    fun getLibVlcEngine(): LibVlcPlayerEngine? = libVlcPlayerEngine
}

/**
 * 保存的播放状态（用于引擎切换时恢复）
 */
private data class SavedPlaybackState(
    val queue: List<MediaSource>,
    val currentIndex: Int,
    val position: Long,
    val playMode: PlayMode,
    val isPlaying: Boolean
)

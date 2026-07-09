package com.webdav.player.core.player

import javax.inject.Inject
import javax.inject.Provider

/**
 * 播放引擎工厂
 *
 * 根据引擎类型创建对应的 PlayerEngine 实现
 *
 * M1 阶段：仅接口定义，具体实现（ExoPlayerEngine）在后续阶段补充
 */
class PlayerEngineFactory @Inject constructor(
    private val engineProviders: Map<EngineType, @JvmSuppressWildcards Provider<PlayerEngine>>
) {
    /**
     * 创建播放引擎
     *
     * @param type 引擎类型
     * @return 对应的引擎实例
     * @throws IllegalArgumentException 不支持的引擎类型
     */
    fun create(type: EngineType): PlayerEngine {
        return engineProviders[type]?.get()
            ?: throw IllegalArgumentException("不支持的播放引擎类型: $type")
    }

    /**
     * 获取默认引擎类型
     */
    fun defaultEngineType(): EngineType = EngineType.EXOPLAYER
}

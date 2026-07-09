package com.webdav.player.di

import com.webdav.player.core.player.EngineManager
import com.webdav.player.core.player.EngineType
import com.webdav.player.core.player.PlayerEngine
import com.webdav.player.core.player.PlayerEngineFactory
import com.webdav.player.core.player.exoplayer.ExoPlayerEngine
import com.webdav.player.core.player.libvlc.LibVlcPlayerEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 播放器依赖注入模块
 *
 * M5: 同时绑定 ExoPlayer 和 LibVLC 引擎，提供 EngineManager
 */
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun providePlayerEngineFactory(
        exoPlayerEngine: ExoPlayerEngine,
        libVlcPlayerEngine: LibVlcPlayerEngine
    ): PlayerEngineFactory {
        val engineProviders: Map<EngineType, @JvmSuppressWildcards Provider<PlayerEngine>> =
            mapOf(
                EngineType.EXOPLAYER to Provider { exoPlayerEngine },
                EngineType.LIBVLC to Provider { libVlcPlayerEngine }
            )
        return PlayerEngineFactory(engineProviders)
    }
}

package com.webdav.player.di

import com.webdav.player.core.player.EngineType
import com.webdav.player.core.player.PlayerEngine
import com.webdav.player.core.player.PlayerEngineFactory
import com.webdav.player.core.player.exoplayer.ExoPlayerEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 播放器依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    @Singleton
    abstract fun bindPlayerEngine(impl: ExoPlayerEngine): PlayerEngine
}

/**
 * 播放器相关 Provides
 */
@Module
@InstallIn(SingletonComponent::class)
object PlayerEngineModule {

    @Provides
    @Singleton
    fun providePlayerEngineFactory(
        exoPlayerEngine: ExoPlayerEngine
    ): PlayerEngineFactory {
        val engineProviders: Map<EngineType, @JvmSuppressWildcards Provider<PlayerEngine>> =
            mapOf(EngineType.EXOPLAYER to Provider { exoPlayerEngine })
        return PlayerEngineFactory(engineProviders)
    }
}

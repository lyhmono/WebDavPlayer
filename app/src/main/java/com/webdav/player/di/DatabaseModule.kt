package com.webdav.player.di

import android.content.Context
import androidx.room.Room
import com.webdav.player.core.database.AppDatabase
import com.webdav.player.core.database.DirectoryCacheDao
import com.webdav.player.core.database.PlaylistDao
import com.webdav.player.core.database.PlaylistItemDao
import com.webdav.player.core.database.ServerConfigDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideServerConfigDao(db: AppDatabase): ServerConfigDao = db.serverConfigDao()

    @Provides
    fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun providePlaylistItemDao(db: AppDatabase): PlaylistItemDao = db.playlistItemDao()

    @Provides
    fun provideDirectoryCacheDao(db: AppDatabase): DirectoryCacheDao = db.directoryCacheDao()
}

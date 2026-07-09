package com.webdav.player.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.webdav.player.core.database.entity.DirectoryCacheEntity
import com.webdav.player.core.database.entity.PlaylistEntity
import com.webdav.player.core.database.entity.PlaylistItemEntity
import com.webdav.player.core.database.entity.ServerConfigEntity

/**
 * 应用数据库
 */
@Database(
    entities = [
        ServerConfigEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        DirectoryCacheEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverConfigDao(): ServerConfigDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistItemDao(): PlaylistItemDao
    abstract fun directoryCacheDao(): DirectoryCacheDao

    companion object {
        const val DATABASE_NAME = "webdav_player.db"
    }
}

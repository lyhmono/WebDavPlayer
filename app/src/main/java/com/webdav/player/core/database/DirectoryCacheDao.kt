package com.webdav.player.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.webdav.player.core.database.entity.DirectoryCacheEntity

@Dao
interface DirectoryCacheDao {

    @Query("SELECT * FROM directory_cache WHERE serverId = :serverId AND path = :path LIMIT 1")
    suspend fun get(serverId: Long, path: String): DirectoryCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DirectoryCacheEntity): Long

    @Query("DELETE FROM directory_cache WHERE serverId = :serverId AND path = :path")
    suspend fun delete(serverId: Long, path: String)

    @Query("DELETE FROM directory_cache WHERE serverId = :serverId")
    suspend fun deleteByServer(serverId: Long)

    @Query("DELETE FROM directory_cache WHERE cachedAt < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM directory_cache")
    suspend fun clearAll()
}

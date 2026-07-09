package com.webdav.player.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.webdav.player.core.database.entity.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistItemDao {

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY sortOrder ASC")
    fun observeByPlaylist(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY sortOrder ASC")
    suspend fun getByPlaylist(playlistId: Long): List<PlaylistItemEntity>

    @Query("SELECT * FROM playlist_items WHERE id = :id")
    suspend fun getById(id: Long): PlaylistItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlaylistItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PlaylistItemEntity>): List<Long>

    @Update
    suspend fun update(entity: PlaylistItemEntity)

    @Delete
    suspend fun delete(entity: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)

    @Query("SELECT MAX(sortOrder) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getMaxSortOrder(playlistId: Long): Int?
}

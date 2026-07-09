package com.webdav.player.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.webdav.player.core.database.entity.ServerConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerConfigDao {

    @Query("SELECT * FROM server_configs ORDER BY isDefault DESC, name ASC")
    fun observeAll(): Flow<List<ServerConfigEntity>>

    @Query("SELECT * FROM server_configs ORDER BY isDefault DESC, name ASC")
    suspend fun getAll(): List<ServerConfigEntity>

    @Query("SELECT * FROM server_configs WHERE id = :id")
    suspend fun getById(id: Long): ServerConfigEntity?

    @Query("SELECT * FROM server_configs WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): ServerConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ServerConfigEntity): Long

    @Update
    suspend fun update(entity: ServerConfigEntity)

    @Delete
    suspend fun delete(entity: ServerConfigEntity)

    @Query("DELETE FROM server_configs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE server_configs SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE server_configs SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: Long)
}

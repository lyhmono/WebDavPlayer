package com.webdav.player.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 播放列表
 */
@Entity(
    tableName = "playlists",
    indices = [Index(value = ["name"], unique = true)]
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 播放列表名称 */
    val name: String,
    /** 描述 */
    val description: String = "",
    /** 创建时间 */
    val createdAt: Long = System.currentTimeMillis(),
    /** 最后更新时间 */
    val updatedAt: Long = System.currentTimeMillis()
)

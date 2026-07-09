package com.webdav.player.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 目录缓存
 *
 * 缓存 WebDAV 目录浏览结果，减少网络请求
 */
@Entity(
    tableName = "directory_cache",
    indices = [
        Index(value = ["serverId", "path"], unique = true)
    ]
)
data class DirectoryCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 服务器配置 ID */
    val serverId: Long,
    /** 目录路径 */
    val path: String,
    /** 目录内容的 JSON 序列化 */
    val contentJson: String,
    /** 缓存时间 */
    val cachedAt: Long = System.currentTimeMillis()
)

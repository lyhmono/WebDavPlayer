package com.webdav.player.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 播放列表项
 */
@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["playlistId", "sortOrder"])
    ]
)
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 所属播放列表 ID */
    val playlistId: Long,
    /** 服务器 ID */
    val serverId: Long,
    /** WebDAV 路径 */
    val path: String,
    /** 显示名称 */
    val displayName: String,
    /** 媒体类型：video / audio */
    val mediaType: String,
    /** 文件大小 */
    val size: Long = 0,
    /** MIME 类型 */
    val mimeType: String? = null,
    /** 排序顺序 */
    val sortOrder: Int = 0,
    /** 添加时间 */
    val addedAt: Long = System.currentTimeMillis()
)

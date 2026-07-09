package com.webdav.player.data.model

/**
 * 播放列表项（领域模型）
 */
data class PlaylistItem(
    val id: Long = 0,
    val playlistId: Long,
    val serverId: Long,
    val path: String,
    val displayName: String,
    val mediaType: MediaType,
    val size: Long = 0,
    val mimeType: String? = null,
    val sortOrder: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

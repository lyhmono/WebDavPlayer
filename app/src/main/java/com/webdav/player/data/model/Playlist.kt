package com.webdav.player.data.model

/**
 * 播放列表（领域模型）
 */
data class Playlist(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val items: List<PlaylistItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

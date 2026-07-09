package com.webdav.player.data.repository

import com.webdav.player.core.database.PlaylistDao
import com.webdav.player.core.database.PlaylistItemDao
import com.webdav.player.core.database.entity.PlaylistEntity
import com.webdav.player.core.database.entity.PlaylistItemEntity
import com.webdav.player.data.model.MediaType
import com.webdav.player.data.model.Playlist
import com.webdav.player.data.model.PlaylistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 播放列表仓库
 */
@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val playlistItemDao: PlaylistItemDao
) {
    fun observeAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.observeAll().map { entities ->
            entities.map { it.toDomain(emptyList()) }
        }
    }

    suspend fun getAllPlaylists(): List<Playlist> {
        return playlistDao.getAll().map { it.toDomain(emptyList()) }
    }

    suspend fun getPlaylist(playlistId: Long): Playlist? {
        val playlist = playlistDao.getById(playlistId) ?: return null
        val items = playlistItemDao.getByPlaylist(playlistId)
        return playlist.toDomain(items.map { it.toDomain() })
    }

    suspend fun createPlaylist(name: String, description: String = ""): Long {
        return playlistDao.insert(PlaylistEntity(name = name, description = description))
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.update(
            PlaylistEntity(
                id = playlist.id,
                name = playlist.name,
                description = playlist.description,
                createdAt = playlist.createdAt,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deleteById(playlistId)
    }

    suspend fun addPlaylistItem(playlistId: Long, item: PlaylistItem): Long {
        val maxOrder = playlistItemDao.getMaxSortOrder(playlistId) ?: 0
        return playlistItemDao.insert(
            PlaylistItemEntity(
                playlistId = playlistId,
                serverId = item.serverId,
                path = item.path,
                displayName = item.displayName,
                mediaType = item.mediaType.name,
                size = item.size,
                mimeType = item.mimeType,
                sortOrder = maxOrder + 1
            )
        )
    }

    suspend fun removePlaylistItem(itemId: Long) {
        playlistItemDao.deleteById(itemId)
    }

    suspend fun reorderPlaylistItem(itemId: Long, newOrder: Int) {
        val item = playlistItemDao.getById(itemId) ?: return
        playlistItemDao.update(
            PlaylistItemEntity(
                id = item.id,
                playlistId = item.playlistId,
                serverId = item.serverId,
                path = item.path,
                displayName = item.displayName,
                mediaType = item.mediaType,
                size = item.size,
                mimeType = item.mimeType,
                sortOrder = newOrder,
                addedAt = item.addedAt
            )
        )
    }

    private fun PlaylistEntity.toDomain(items: List<PlaylistItem>): Playlist {
        return Playlist(
            id = id,
            name = name,
            description = description,
            items = items,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun PlaylistItemEntity.toDomain(): PlaylistItem {
        return PlaylistItem(
            id = id,
            playlistId = playlistId,
            serverId = serverId,
            path = path,
            displayName = displayName,
            mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrDefault(MediaType.UNKNOWN),
            size = size,
            mimeType = mimeType,
            sortOrder = sortOrder,
            addedAt = addedAt
        )
    }
}

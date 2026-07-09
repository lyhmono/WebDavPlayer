package com.webdav.player.feature.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webdav.player.core.player.MediaSource
import com.webdav.player.core.player.exoplayer.ExoPlayerEngine
import com.webdav.player.data.model.MediaType
import com.webdav.player.data.model.Playlist
import com.webdav.player.data.model.PlaylistItem
import com.webdav.player.data.repository.PlaylistRepository
import com.webdav.player.feature.player.PlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 播放列表管理 ViewModel
 */
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        loadPlaylists()
    }

    /**
     * 加载所有播放列表
     */
    fun loadPlaylists() {
        viewModelScope.launch {
            playlistRepository.observeAllPlaylists().collect { playlists ->
                _uiState.update { it.copy(playlists = playlists, isLoading = false) }
            }
        }
    }

    /**
     * 创建播放列表
     */
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            try {
                playlistRepository.createPlaylist(name)
                _uiState.update { it.copy(actionResult = "播放列表 \"$name\" 已创建") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "创建失败: ${e.message}") }
            }
        }
    }

    /**
     * 删除播放列表
     */
    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            try {
                playlistRepository.deletePlaylist(playlistId)
                _uiState.update { it.copy(actionResult = "播放列表已删除") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "删除失败: ${e.message}") }
            }
        }
    }

    /**
     * 获取播放列表详情
     */
    suspend fun getPlaylistDetail(playlistId: Long): Playlist? {
        return withContext(Dispatchers.IO) {
            playlistRepository.getPlaylist(playlistId)
        }
    }

    /**
     * 从播放列表中移除项目
     */
    fun removePlaylistItem(itemId: Long) {
        viewModelScope.launch {
            try {
                playlistRepository.removePlaylistItem(itemId)
                _uiState.update { it.copy(actionResult = "已移除") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "移除失败: ${e.message}") }
            }
        }
    }

    /**
     * 添加文件到播放列表
     */
    fun addToPlaylist(
        playlistId: Long,
        serverId: Long,
        path: String,
        displayName: String,
        mediaType: MediaType,
        size: Long = 0,
        mimeType: String? = null
    ) {
        viewModelScope.launch {
            try {
                val item = PlaylistItem(
                    playlistId = playlistId,
                    serverId = serverId,
                    path = path,
                    displayName = displayName,
                    mediaType = mediaType,
                    size = size,
                    mimeType = mimeType
                )
                playlistRepository.addPlaylistItem(playlistId, item)
                _uiState.update { it.copy(actionResult = "已添加到播放列表") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "添加失败: ${e.message}") }
            }
        }
    }

    /**
     * 从当前播放队列保存为播放列表
     */
    fun saveQueueAsPlaylist(playerViewModel: PlayerViewModel, name: String) {
        viewModelScope.launch {
            val result = playerViewModel.saveQueueAsPlaylist(name)
            result.onSuccess {
                _uiState.update { it.copy(actionResult = "播放列表已保存") }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = "保存失败: ${e.message}") }
            }
        }
    }

    fun clearActionResult() {
        _uiState.update { it.copy(actionResult = null, errorMessage = null) }
    }
}

/**
 * 播放列表 UI 状态
 */
data class PlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionResult: String? = null
)

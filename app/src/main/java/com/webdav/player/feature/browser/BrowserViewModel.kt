package com.webdav.player.feature.browser

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webdav.player.core.cache.DirectoryCacheManager
import com.webdav.player.core.cache.ThumbnailCacheManager
import com.webdav.player.data.model.ServerConfig
import com.webdav.player.data.model.WebDavEntry
import com.webdav.player.data.preferences.AppPreferences
import com.webdav.player.data.repository.ServerConfigRepository
import com.webdav.player.data.repository.WebDavRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 文件浏览器 ViewModel
 */
@HiltViewModel
class BrowserViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverConfigRepository: ServerConfigRepository,
    private val webDavRepository: WebDavRepository,
    private val directoryCacheManager: DirectoryCacheManager,
    private val thumbnailCacheManager: ThumbnailCacheManager,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    /** 当前服务器配置 */
    private var serverConfig: ServerConfig? = null

    /** 当前正在进行的加载任务 */
    private var loadJob: Job? = null

    init {
        // 从偏好设置加载默认排序
        viewModelScope.launch {
            appPreferences.defaultSortField.collect { field ->
                _uiState.update { it.copy(sortField = field) }
            }
        }
        viewModelScope.launch {
            appPreferences.defaultSortAscending.collect { ascending ->
                _uiState.update { it.copy(sortAscending = ascending) }
            }
        }
        viewModelScope.launch {
            appPreferences.directoriesFirst.collect { dirsFirst ->
                _uiState.update { it.copy(directoriesFirst = dirsFirst) }
            }
        }
    }

    /**
     * 初始化：加载服务器配置和目录
     *
     * @param serverId 服务器 ID
     * @param encodedPath 编码后的路径（URL 编码）
     */
    fun initialize(serverId: Long, encodedPath: String) {
        val path = java.net.URLDecoder.decode(encodedPath, "UTF-8")
        viewModelScope.launch {
            val config = serverConfigRepository.getById(serverId)
            if (config == null) {
                _uiState.update { it.copy(errorMessage = "服务器配置不存在") }
                return@launch
            }
            serverConfig = config
            loadDirectory(path, forceRefresh = false)
        }
    }

    /**
     * 加载目录内容
     *
     * 策略：缓存优先 → 后台刷新
     *
     * @param path 目录路径
     * @param forceRefresh 是否强制刷新（忽略缓存）
     */
    fun loadDirectory(path: String, forceRefresh: Boolean = false) {
        val config = serverConfig ?: return
        val normalizedPath = normalizePath(path)

        // 取消之前的加载任务
        loadJob?.cancel()

        _uiState.update {
            it.copy(
                currentPath = normalizedPath,
                isLoading = true,
                isRefreshing = forceRefresh
            )
        }

        loadJob = viewModelScope.launch {
            // 1. 先尝试缓存
            if (!forceRefresh) {
                val cached = directoryCacheManager.getCachedEvenIfExpired(config.id, normalizedPath)
                if (cached != null) {
                    _uiState.update {
                        it.copy(
                            entries = cached,
                            isLoading = false,
                            isFromCache = true
                        )
                    }
                    // 后台刷新
                    refreshFromNetwork(config, normalizedPath, isBackgroundRefresh = true)
                    return@launch
                }
            }

            // 2. 从网络加载
            refreshFromNetwork(config, normalizedPath, isBackgroundRefresh = false)
        }
    }

    /**
     * 从网络刷新目录内容
     */
    private suspend fun refreshFromNetwork(
        config: ServerConfig,
        path: String,
        isBackgroundRefresh: Boolean
    ) {
        val result = webDavRepository.listDirectory(config, path)

        result.onSuccess { entries ->
            // 更新缓存
            directoryCacheManager.refresh(config.id, path, entries)

            _uiState.update {
                it.copy(
                    entries = entries,
                    isLoading = false,
                    isRefreshing = false,
                    isFromCache = false,
                    errorMessage = null
                )
            }
        }.onFailure { error ->
            if (isBackgroundRefresh) {
                // 后台刷新失败，保持缓存数据，仅提示
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = "刷新失败: ${error.message}"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "加载失败: ${error.message}"
                    )
                }
            }
        }
    }

    /**
     * 下拉刷新
     */
    fun refresh() {
        val currentPath = _uiState.value.currentPath
        loadDirectory(currentPath, forceRefresh = true)
    }

    /**
     * 导航到子目录
     */
    fun navigateToDirectory(path: String) {
        loadDirectory(path, forceRefresh = false)
    }

    /**
     * 返回上级目录
     */
    fun navigateUp() {
        val currentPath = _uiState.value.currentPath
        if (currentPath == "/" || currentPath.isEmpty()) return

        val parentPath = currentPath.trimEnd('/').substringBeforeLast('/', "/")
        val normalizedParent = if (parentPath.isEmpty()) "/" else parentPath
        loadDirectory(normalizedParent, forceRefresh = false)
    }

    // ============ 排序 ============

    /**
     * 设置排序字段
     */
    fun setSortField(field: SortField) {
        _uiState.update { it.copy(sortField = field) }
        applySorting()
        viewModelScope.launch { appPreferences.setDefaultSortField(field) }
    }

    /**
     * 切换排序方向
     */
    fun toggleSortOrder() {
        val newAscending = !_uiState.value.sortAscending
        _uiState.update { it.copy(sortAscending = newAscending) }
        applySorting()
        viewModelScope.launch { appPreferences.setDefaultSortAscending(newAscending) }
    }

    /**
     * 切换目录优先
     */
    fun toggleDirectoriesFirst() {
        val newDirsFirst = !_uiState.value.directoriesFirst
        _uiState.update { it.copy(directoriesFirst = newDirsFirst) }
        applySorting()
        viewModelScope.launch { appPreferences.setDirectoriesFirst(newDirsFirst) }
    }

    /**
     * 应用排序
     */
    private fun applySorting() {
        val state = _uiState.value
        val sorted = sortEntries(state.entries, state.sortField, state.sortAscending, state.directoriesFirst)
        _uiState.update { it.copy(entries = sorted) }
    }

    // ============ 搜索 ============

    /**
     * 设置搜索关键词
     */
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * 获取过滤后的文件列表
     */
    fun getFilteredEntries(): List<WebDavEntry> {
        val state = _uiState.value
        val query = state.searchQuery.trim()
        return if (query.isEmpty()) {
            state.entries
        } else {
            state.entries.filter { it.displayName.contains(query, ignoreCase = true) }
        }
    }

    // ============ 多选 ============

    /**
     * 进入多选模式
     */
    fun enterSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = true) }
    }

    /**
     * 退出多选模式
     */
    fun exitSelectionMode() {
        _uiState.update {
            it.copy(
                isSelectionMode = false,
                selectedPaths = emptySet()
            )
        }
    }

    /**
     * 切换选中状态
     */
    fun toggleSelection(path: String) {
        _uiState.update { state ->
            val newSelected = if (path in state.selectedPaths) {
                state.selectedPaths - path
            } else {
                state.selectedPaths + path
            }
            // 如果取消了所有选中项，自动退出多选模式
            state.copy(
                selectedPaths = newSelected,
                isSelectionMode = newSelected.isNotEmpty()
            )
        }
    }

    /**
     * 全选当前目录
     */
    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedPaths = state.entries.map { it.path }.toSet())
        }
    }

    // ============ 文件操作 ============

    /**
     * 删除选中文件
     */
    fun deleteSelected() {
        val config = serverConfig ?: return
        val selectedPaths = _uiState.value.selectedPaths

        if (selectedPaths.isEmpty()) return

        _uiState.update { it.copy(isPerformingAction = true) }

        viewModelScope.launch {
            var successCount = 0
            var failedCount = 0

            for (path in selectedPaths) {
                val result = webDavRepository.deleteFile(config, path)
                if (result.isSuccess) successCount++ else failedCount++
            }

            _uiState.update {
                it.copy(
                    isPerformingAction = false,
                    isSelectionMode = false,
                    selectedPaths = emptySet()
                )
            }

            // 清除缓存并刷新
            directoryCacheManager.clear(config.id, _uiState.value.currentPath)
            refresh()

            val msg = if (failedCount == 0) {
                "已删除 $successCount 项"
            } else {
                "成功 $successCount 项，失败 $failedCount 项"
            }
            _uiState.update { it.copy(actionResult = msg) }
        }
    }

    /**
     * 重命名文件
     */
    fun renameFile(oldPath: String, newName: String) {
        val config = serverConfig ?: return

        viewModelScope.launch {
            val parentPath = oldPath.trimEnd('/').substringBeforeLast('/', "/")
            val newPath = if (parentPath.endsWith("/")) "$parentPath$newName" else "$parentPath/$newName"

            val result = webDavRepository.renameFile(config, oldPath, newPath)
            if (result.isSuccess) {
                directoryCacheManager.clear(config.id, _uiState.value.currentPath)
                refresh()
                _uiState.update { it.copy(actionResult = "重命名成功") }
            } else {
                _uiState.update { it.copy(errorMessage = "重命名失败: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    // ============ 上传 ============

    /**
     * 上传文件
     *
     * @param uri 文件 URI
     */
    fun uploadFile(uri: Uri) {
        val config = serverConfig ?: return
        val currentPath = _uiState.value.currentPath

        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadProgress = 0f) }

            try {
                val fileName = getFileName(uri) ?: "unknown_file"
                val targetPath = if (currentPath.endsWith("/")) "$currentPath$fileName" else "$currentPath/$fileName"
                val contentType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            errorMessage = "无法读取文件"
                        )
                    }
                    return@launch
                }

                val result = webDavRepository.uploadFile(
                    serverConfig = config,
                    path = targetPath,
                    inputStream = inputStream,
                    contentType = contentType
                ) { progress ->
                    _uiState.update { it.copy(uploadProgress = progress) }
                }

                inputStream.close()

                if (result.isSuccess) {
                    directoryCacheManager.clear(config.id, currentPath)
                    refresh()
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadProgress = 0f,
                            actionResult = "上传成功"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadProgress = 0f,
                            errorMessage = "上传失败: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadProgress = 0f,
                        errorMessage = "上传失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 取消上传
     */
    fun cancelUpload() {
        _uiState.update {
            it.copy(
                isUploading = false,
                uploadProgress = 0f
            )
        }
    }

    // ============ 缩略图 ============

    /**
     * 获取视频缩略图路径
     */
    suspend fun getVideoThumbnail(entry: WebDavEntry): String? {
        val config = serverConfig ?: return null
        return thumbnailCacheManager.getVideoThumbnail(config, entry)
    }

    // ============ 播放相关 ============

    /**
     * 获取当前服务器 ID
     */
    fun getCurrentServerId(): Long? = serverConfig?.id

    /**
     * 获取当前目录所有可播放文件
     */
    fun getPlayableEntries(): List<WebDavEntry> {
        return _uiState.value.entries.filter { it.isPlayable }
    }

    // ============ 辅助方法 ============

    /**
     * 获取面包屑路径层级
     */
    fun getBreadcrumbs(): List<Breadcrumb> {
        val path = _uiState.value.currentPath.trim('/')
        if (path.isEmpty()) return listOf(Breadcrumb("根目录", "/"))

        val parts = path.split("/")
        val breadcrumbs = mutableListOf(Breadcrumb("根目录", "/"))
        var currentPath = ""

        for (part in parts) {
            currentPath = if (currentPath.isEmpty()) "/$part" else "$currentPath/$part"
            breadcrumbs.add(Breadcrumb(part, currentPath))
        }

        return breadcrumbs
    }

    /**
     * 获取文件名
     */
    private fun getFileName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && it.moveToFirst()) {
                return it.getString(nameIndex)
            }
        }
        return uri.lastPathSegment
    }

    /**
     * 清除操作结果消息
     */
    fun clearActionResult() {
        _uiState.update { it.copy(actionResult = null) }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ============ 静态方法 ============

    companion object {
        fun normalizePath(path: String): String {
            var p = path.trim()
            if (!p.startsWith("/")) p = "/$p"
            return p
        }

        fun sortEntries(
            entries: List<WebDavEntry>,
            field: SortField,
            ascending: Boolean,
            directoriesFirst: Boolean
        ): List<WebDavEntry> {
            val sorted = if (directoriesFirst) {
                val (dirs, files) = entries.partition { it.isDirectory }
                val sortedDirs = sortList(dirs, field, ascending)
                val sortedFiles = sortList(files, field, ascending)
                sortedDirs + sortedFiles
            } else {
                sortList(entries, field, ascending)
            }
            return sorted
        }

        private fun sortList(
            entries: List<WebDavEntry>,
            field: SortField,
            ascending: Boolean
        ): List<WebDavEntry> {
            val comparator = when (field) {
                SortField.NAME -> compareBy { it.displayName.lowercase() }
                SortField.SIZE -> compareBy { it.size }
                SortField.MODIFIED -> compareBy { it.lastModified }
                SortField.TYPE -> compareBy { it.extension }
            }
            return if (ascending) entries.sortedWith(comparator) else entries.sortedWith(comparator.reversed())
        }
    }
}

// ============ 数据类 ============

/**
 * 排序字段
 */
enum class SortField(val displayName: String) {
    NAME("名称"),
    SIZE("大小"),
    MODIFIED("修改时间"),
    TYPE("类型")
}

/**
 * 面包屑
 */
data class Breadcrumb(
    val name: String,
    val path: String
)

/**
 * 浏览器 UI 状态
 */
data class BrowserUiState(
    val currentPath: String = "/",
    val entries: List<WebDavEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isFromCache: Boolean = false,
    val errorMessage: String? = null,
    val actionResult: String? = null,
    // 排序
    val sortField: SortField = SortField.NAME,
    val sortAscending: Boolean = true,
    val directoriesFirst: Boolean = true,
    // 搜索
    val searchQuery: String = "",
    // 多选
    val isSelectionMode: Boolean = false,
    val selectedPaths: Set<String> = emptySet(),
    // 上传
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    // 操作中
    val isPerformingAction: Boolean = false
)

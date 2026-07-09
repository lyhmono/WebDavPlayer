package com.webdav.player.feature.browser

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.webdav.player.data.model.WebDavEntry
import com.webdav.player.feature.browser.components.BreadcrumbNav
import com.webdav.player.feature.browser.components.FileListItem
import com.webdav.player.feature.browser.components.RenameDialog
import com.webdav.player.feature.browser.components.SearchBar
import com.webdav.player.feature.browser.components.SelectionActionBar
import com.webdav.player.feature.browser.components.SortBar
import com.webdav.player.feature.browser.components.UploadDialog

/**
 * 文件浏览器界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    serverId: Long,
    encodedPath: String,
    onBack: () -> Unit,
    onPlayFile: (WebDavEntry) -> Unit = {},
    onPlayAll: (List<WebDavEntry>, Int) -> Unit = { _, _ -> },
    onNavigateToPlayer: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    playerViewModel: com.webdav.player.feature.player.PlayerViewModel? = null,
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showSearch by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<WebDavEntry?>(null) }

    // 初始化
    LaunchedEffect(serverId, encodedPath) {
        viewModel.initialize(serverId, encodedPath)
    }

    // 显示错误消息
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // 显示操作结果
    LaunchedEffect(uiState.actionResult) {
        uiState.actionResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionResult()
        }
    }

    // 如果路径变化，更新 URL 参数
    LaunchedEffect(uiState.currentPath) {
        // 路径变化时可以更新导航参数（可选）
    }

    val filteredEntries = remember(uiState.entries, uiState.searchQuery, uiState.sortField, uiState.sortAscending, uiState.directoriesFirst) {
        val baseList = if (uiState.searchQuery.isBlank()) {
            uiState.entries
        } else {
            uiState.entries.filter { it.displayName.contains(uiState.searchQuery, ignoreCase = true) }
        }
        BrowserViewModel.sortEntries(
            baseList,
            uiState.sortField,
            uiState.sortAscending,
            uiState.directoriesFirst
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSelectionMode) {
                        Text("已选 ${uiState.selectedPaths.size} 项")
                    } else {
                        Text("文件浏览")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isSelectionMode) {
                            viewModel.exitSelectionMode()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (uiState.isSelectionMode) "退出多选" else "返回"
                        )
                    }
                },
                actions = {
                    if (!uiState.isSelectionMode) {
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索"
                            )
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.isSelectionMode && !uiState.isUploading) {
                FloatingActionButton(
                    onClick = { showUploadDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "上传文件"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 面包屑导航
            if (!uiState.isSelectionMode) {
                BreadcrumbNav(
                    breadcrumbs = viewModel.getBreadcrumbs(),
                    onNavigate = { path -> viewModel.navigateToDirectory(path) }
                )
            }

            // 搜索栏
            if (!uiState.isSelectionMode) {
                SearchBar(
                    isVisible = showSearch,
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onClose = { showSearch = false }
                )
            }

            // 排序栏
            if (!uiState.isSelectionMode && filteredEntries.isNotEmpty()) {
                SortBar(
                    currentField = uiState.sortField,
                    ascending = uiState.sortAscending,
                    directoriesFirst = uiState.directoriesFirst,
                    onFieldSelected = { viewModel.setSortField(it) },
                    onToggleOrder = { viewModel.toggleSortOrder() },
                    onToggleDirectoriesFirst = { viewModel.toggleDirectoriesFirst() }
                )
            }

            // 内容区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    // 加载中（无缓存数据）
                    uiState.isLoading && filteredEntries.isEmpty() -> {
                        LoadingState()
                    }
                    // 空目录
                    filteredEntries.isEmpty() && !uiState.isLoading -> {
                        EmptyState(isSearching = uiState.searchQuery.isNotBlank())
                    }
                    // 文件列表
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(
                                items = filteredEntries,
                                key = { it.path }
                            ) { entry ->
                                FileListItem(
                                    entry = entry,
                                    isSelected = entry.path in uiState.selectedPaths,
                                    isSelectionMode = uiState.isSelectionMode,
                                    onClick = {
                                        handleFileClick(
                                            entry = entry,
                                            viewModel = viewModel,
                                            context = context,
                                            onRenameRequest = { renameTarget = it },
                                            onPlayFile = onPlayFile,
                                            onNavigateToPlayer = onNavigateToPlayer
                                        )
                                    },
                                    onLongClick = {
                                        if (!uiState.isSelectionMode) {
                                            viewModel.enterSelectionMode()
                                        }
                                        viewModel.toggleSelection(entry.path)
                                    }
                                )
                            }
                        }

                        // 后台刷新指示器
                        if (uiState.isRefreshing) {
                            androidx.compose.material3.LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                            )
                        }
                    }
                }
            }

            // 多选操作栏
            if (uiState.isSelectionMode && uiState.selectedPaths.isNotEmpty()) {
                SelectionActionBar(
                    selectedCount = uiState.selectedPaths.size,
                    onDelete = { viewModel.deleteSelected() },
                    onRename = {
                        // 找到第一个选中的文件进行重命名
                        val firstSelected = filteredEntries.find { it.path in uiState.selectedPaths }
                        if (firstSelected != null) {
                            renameTarget = firstSelected
                        }
                    },
                    onAddToPlaylist = {
                        Toast.makeText(context, "加入播放列表功能将在后续版本提供", Toast.LENGTH_SHORT).show()
                    },
                    onSelectAll = { viewModel.selectAll() },
                    onCancel = { viewModel.exitSelectionMode() }
                )
            }

            // 迷你播放器条
            if (playerViewModel != null) {
                com.webdav.player.feature.player.MiniPlayerBar(
                    viewModel = playerViewModel,
                    onClick = onNavigateToPlayer
                )
            }
        }
    }

    // 上传对话框
    UploadDialog(
        isVisible = showUploadDialog,
        isUploading = uiState.isUploading,
        uploadProgress = uiState.uploadProgress,
        onFileSelected = { uri ->
            viewModel.uploadFile(uri)
        },
        onCancel = {
            viewModel.cancelUpload()
        },
        onDismiss = {
            showUploadDialog = false
        }
    )

    // 重命名对话框
    renameTarget?.let { entry ->
        RenameDialog(
            isVisible = true,
            currentName = entry.displayName,
            onConfirm = { newName ->
                viewModel.renameFile(entry.path, newName)
                renameTarget = null
            },
            onDismiss = {
                renameTarget = null
            }
        )
    }
}

// ============ 事件处理 ============

/**
 * 处理文件点击事件
 */
private fun handleFileClick(
    entry: WebDavEntry,
    viewModel: BrowserViewModel,
    context: android.content.Context,
    onRenameRequest: (WebDavEntry) -> Unit,
    onPlayFile: (WebDavEntry) -> Unit = {},
    onNavigateToPlayer: () -> Unit = {}
) {
    val state = viewModel.uiState.value

    if (state.isSelectionMode) {
        viewModel.toggleSelection(entry.path)
        return
    }

    if (entry.isDirectory) {
        viewModel.navigateToDirectory(entry.path)
    } else if (entry.isPlayable) {
        // M3: 调用播放器播放
        onPlayFile(entry)
        onNavigateToPlayer()
    } else {
        android.widget.Toast.makeText(
            context,
            "不支持的文件类型: ${entry.displayName}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

// ============ 状态组件 ============

/**
 * 加载中状态
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
            Text(
                text = "加载中...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 空目录状态
 */
@Composable
private fun EmptyState(isSearching: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isSearching) Icons.Default.Search else Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Text(
                text = if (isSearching) "未找到匹配的文件" else "空目录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!isSearching) {
                Text(
                    text = "点击右下角按钮上传文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

package com.webdav.player.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Caching
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.webdav.player.core.player.EngineType
import com.webdav.player.feature.browser.SortField
import com.webdav.player.ui.theme.ThemeMode

/**
 * 设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val playerEngine by viewModel.playerEngine.collectAsState()
    val autoPlayNext by viewModel.autoPlayNext.collectAsState()
    val defaultPlaybackSpeed by viewModel.defaultPlaybackSpeed.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val defaultSortField by viewModel.defaultSortField.collectAsState()
    val defaultSortAscending by viewModel.defaultSortAscending.collectAsState()
    val directoriesFirst by viewModel.directoriesFirst.collectAsState()
    val cacheExpireMinutes by viewModel.cacheExpireMinutes.collectAsState()
    val thumbnailCacheEnabled by viewModel.thumbnailCacheEnabled.collectAsState()
    val gestureControl by viewModel.gestureControl.collectAsState()
    val trustSelfSignedCert by viewModel.trustSelfSignedCert.collectAsState()

    var showClearCacheDialog by remember { mutableStateOf(false) }
    val cacheSize = remember { viewModel.getCacheSize() }
    val cacheSizeText = formatCacheSize(cacheSize)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ============ 播放设置 ============
            SettingsGroupCard(
                title = "播放设置",
                icon = Icons.Default.PlayCircle
            ) {
                // 播放引擎选择
                Text(
                    text = "播放引擎",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                EngineType.entries.forEach { engine ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (engine == EngineType.EXOPLAYER) {
                                    viewModel.setPlayerEngine(engine)
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = playerEngine == engine,
                            onClick = {
                                if (engine == EngineType.EXOPLAYER) {
                                    viewModel.setPlayerEngine(engine)
                                }
                            },
                            enabled = engine == EngineType.EXOPLAYER
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = engineDisplayName(engine),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (engine != EngineType.EXOPLAYER) {
                                Text(
                                    text = "即将支持",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 自动播放下一首
                SwitchSettingItem(
                    title = "自动播放下一首",
                    checked = autoPlayNext,
                    onCheckedChange = { viewModel.setAutoPlayNext(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 默认播放速度
                Text(
                    text = "默认播放速度",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                PlaybackSpeedSelector(
                    currentSpeed = defaultPlaybackSpeed,
                    onSpeedSelected = { viewModel.setDefaultPlaybackSpeed(it) }
                )
            }

            // ============ 外观设置 ============
            SettingsGroupCard(
                title = "外观设置",
                icon = Icons.Default.Palette
            ) {
                // 主题模式
                Text(
                    text = "主题模式",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                        ) {
                            Text(mode.displayName)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 动态色彩
                SwitchSettingItem(
                    title = "动态色彩",
                    subtitle = "跟随系统壁纸自动配色（Android 12+）",
                    checked = dynamicColor,
                    onCheckedChange = { viewModel.setDynamicColor(it) }
                )
            }

            // ============ 浏览设置 ============
            SettingsGroupCard(
                title = "浏览设置",
                icon = Icons.Default.Sort
            ) {
                // 默认排序字段
                Text(
                    text = "默认排序字段",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                DropdownSelector(
                    options = SortField.entries.map { it.displayName to it },
                    selectedOption = defaultSortField,
                    onOptionSelected = { viewModel.setDefaultSortField(it) }
                )

                Spacer(Modifier.height(12.dp))

                // 默认排序方向
                Text(
                    text = "默认排序方向",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = defaultSortAscending,
                        onClick = { viewModel.setDefaultSortAscending(true) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("升序") }
                    SegmentedButton(
                        selected = !defaultSortAscending,
                        onClick = { viewModel.setDefaultSortAscending(false) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("降序") }
                }

                Spacer(Modifier.height(12.dp))

                // 目录优先
                SwitchSettingItem(
                    title = "目录优先",
                    subtitle = "目录始终显示在文件之前",
                    checked = directoriesFirst,
                    onCheckedChange = { viewModel.setDirectoriesFirst(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 缓存过期时间
                Text(
                    text = "缓存过期时间",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                DropdownSelector(
                    options = listOf(
                        "1 分钟" to 1,
                        "5 分钟" to 5,
                        "10 分钟" to 10,
                        "30 分钟" to 30,
                        "永不过期" to -1
                    ),
                    selectedOption = cacheExpireMinutes,
                    onOptionSelected = { viewModel.setCacheExpireMinutes(it) }
                )
            }

            // ============ 缓存管理 ============
            SettingsGroupCard(
                title = "缓存管理",
                icon = Icons.Default.Caching
            ) {
                // 缩略图缓存开关
                SwitchSettingItem(
                    title = "缩略图缓存",
                    subtitle = "缓存视频缩略图以加快加载速度",
                    checked = thumbnailCacheEnabled,
                    onCheckedChange = { viewModel.setThumbnailCacheEnabled(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 清除缓存
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearCacheDialog = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "清除缩略图缓存",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "当前缓存大小: $cacheSizeText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // ============ 高级 ============
            SettingsGroupCard(
                title = "高级",
                icon = Icons.Default.Security
            ) {
                // 手势控制
                SwitchSettingItem(
                    title = "手势控制",
                    subtitle = "播放界面滑动手势调节亮度/音量/进度",
                    checked = gestureControl,
                    onCheckedChange = { viewModel.setGestureControl(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 自签证书信任
                SwitchSettingItem(
                    title = "信任自签名证书",
                    subtitle = "允许连接使用自签名 SSL 证书的服务器",
                    checked = trustSelfSignedCert,
                    onCheckedChange = { viewModel.setTrustSelfSignedCert(it) }
                )
            }

            // ============ 关于 ============
            SettingsGroupCard(
                title = "关于",
                icon = Icons.Default.Info
            ) {
                AboutItem(label = "版本", value = "${viewModel.appVersion} (${viewModel.appVersionCode})")
                AboutItem(label = "开源地址", value = viewModel.githubUrl)
                AboutItem(label = "技术栈", value = viewModel.techStack)
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // 清除缓存确认对话框
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("清除缓存") },
            text = { Text("确定要清除缩略图缓存吗？($cacheSizeText)\n\n清除后浏览文件时将重新从服务器获取缩略图。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCache()
                        showClearCacheDialog = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// ============ 可复用组件 ============

/**
 * 设置分组卡片
 */
@Composable
private fun SettingsGroupCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable Column.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/**
 * 开关设置项
 */
@Composable
private fun SwitchSettingItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * 下拉选择器
 */
@Composable
private fun <T> DropdownSelector(
    options: List<Pair<String, T>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.second == selectedOption }?.first ?: ""

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = { },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            enabled = false,
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Text("▼", style = MaterialTheme.typography.bodySmall)
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (label, value) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onOptionSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * 播放速度选择器
 */
@Composable
private fun PlaybackSpeedSelector(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val speedLabels = listOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x")

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        speeds.forEachIndexed { index, speed ->
            SegmentedButton(
                selected = currentSpeed == speed,
                onClick = { onSpeedSelected(speed) },
                shape = SegmentedButtonDefaults.itemShape(index, speeds.size)
            ) {
                Text(speedLabels[index])
            }
        }
    }
}

/**
 * 关于信息项
 */
@Composable
private fun AboutItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Code,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// ============ 辅助函数 ============

/**
 * 获取引擎显示名称
 */
private fun engineDisplayName(engine: EngineType): String = when (engine) {
    EngineType.EXOPLAYER -> "ExoPlayer (Media3)"
    EngineType.LIBVLC -> "LibVLC (VLC)"
    EngineType.SYSTEM -> "系统 MediaPlayer"
}

/**
 * 格式化缓存大小
 */
private fun formatCacheSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

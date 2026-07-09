package com.webdav.player.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webdav.player.BuildConfig
import com.webdav.player.core.cache.ThumbnailCacheManager
import com.webdav.player.core.player.EngineManager
import com.webdav.player.core.player.EngineType
import com.webdav.player.data.preferences.AppPreferences
import com.webdav.player.feature.browser.SortField
import com.webdav.player.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页面 ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val thumbnailCacheManager: ThumbnailCacheManager,
    private val engineManager: EngineManager
) : ViewModel() {

    // ============ 播放设置 ============

    val playerEngine: StateFlow<EngineType> = appPreferences.playerEngine
        .stateIn(viewModelScope, SharingStarted.Eagerly, EngineType.EXOPLAYER)

    /** 当前引擎类型（来自 EngineManager，实时反映切换状态） */
    val currentEngineType: StateFlow<EngineType> = engineManager.currentEngineType
        .stateIn(viewModelScope, SharingStarted.Eagerly, EngineType.EXOPLAYER)

    /** 可用引擎列表 */
    val availableEngines: List<EngineType> = engineManager.availableEngines

    val autoPlayNext: StateFlow<Boolean> = appPreferences.autoPlayNext
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val defaultPlaybackSpeed: StateFlow<Float> = appPreferences.defaultPlaybackSpeed
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    // ============ 外观设置 ============

    val themeMode: StateFlow<ThemeMode> = appPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val dynamicColor: StateFlow<Boolean> = appPreferences.dynamicColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // ============ 浏览设置 ============

    val defaultSortField: StateFlow<SortField> = appPreferences.defaultSortField
        .stateIn(viewModelScope, SharingStarted.Eagerly, SortField.NAME)

    val defaultSortAscending: StateFlow<Boolean> = appPreferences.defaultSortAscending
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val directoriesFirst: StateFlow<Boolean> = appPreferences.directoriesFirst
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val cacheExpireMinutes: StateFlow<Int> = appPreferences.cacheExpireMinutes
        .stateIn(viewModelScope, SharingStarted.Eagerly, 5)

    // ============ 缓存设置 ============

    val thumbnailCacheEnabled: StateFlow<Boolean> = appPreferences.thumbnailCacheEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // ============ 高级设置 ============

    val gestureControl: StateFlow<Boolean> = appPreferences.gestureControl
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val trustSelfSignedCert: StateFlow<Boolean> = appPreferences.trustSelfSignedCert
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ============ 缓存大小 ============

    /**
     * 获取缩略图磁盘缓存大小（字节）
     */
    fun getCacheSize(): Long {
        return thumbnailCacheManager.getDiskCacheSize()
    }

    /**
     * 清除缩略图缓存
     */
    fun clearCache() {
        viewModelScope.launch {
            thumbnailCacheManager.clearAll()
        }
    }

    // ============ 修改方法 ============

    fun setPlayerEngine(engine: EngineType) {
        viewModelScope.launch {
            appPreferences.setPlayerEngine(engine)
            // M5: 实际切换引擎
            engineManager.switchEngine(engine)
        }
    }

    /**
     * 切换播放引擎（M5）
     */
    fun switchEngine(engine: EngineType) {
        viewModelScope.launch {
            appPreferences.setPlayerEngine(engine)
            engineManager.switchEngine(engine)
        }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setAutoPlayNext(enabled) }
    }

    fun setDefaultPlaybackSpeed(speed: Float) {
        viewModelScope.launch { appPreferences.setDefaultPlaybackSpeed(speed) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setDynamicColor(enabled) }
    }

    fun setDefaultSortField(field: SortField) {
        viewModelScope.launch { appPreferences.setDefaultSortField(field) }
    }

    fun setDefaultSortAscending(ascending: Boolean) {
        viewModelScope.launch { appPreferences.setDefaultSortAscending(ascending) }
    }

    fun setDirectoriesFirst(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setDirectoriesFirst(enabled) }
    }

    fun setCacheExpireMinutes(minutes: Int) {
        viewModelScope.launch { appPreferences.setCacheExpireMinutes(minutes) }
    }

    fun setThumbnailCacheEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setThumbnailCacheEnabled(enabled) }
    }

    fun setGestureControl(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setGestureControl(enabled) }
    }

    fun setTrustSelfSignedCert(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setTrustSelfSignedCert(enabled) }
    }

    // ============ 关于信息 ============

    /** 应用版本号 */
    val appVersion: String get() = BuildConfig.VERSION_NAME

    /** 应用版本码 */
    val appVersionCode: Int get() = BuildConfig.VERSION_CODE

    /** GitHub 开源地址 */
    val githubUrl: String = "https://github.com/webdav-player/WebDavPlayer"

    /** 技术栈描述 */
    val techStack: String = "Jetpack Compose · Media3/ExoPlayer · Hilt · DataStore · Room · OkHttp"
}

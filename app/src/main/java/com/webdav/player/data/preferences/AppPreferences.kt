package com.webdav.player.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.webdav.player.core.player.EngineType
import com.webdav.player.feature.browser.SortField
import com.webdav.player.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用偏好设置管理器
 *
 * 使用 DataStore Preferences 存储用户设置
 */
@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    // ============ Preference Keys ============

    private object Keys {
        // 播放设置
        val PLAYER_ENGINE = stringPreferencesKey("player_engine")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")

        // 外观设置
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")

        // 浏览设置
        val DEFAULT_SORT_FIELD = stringPreferencesKey("default_sort_field")
        val DEFAULT_SORT_ASCENDING = booleanPreferencesKey("default_sort_ascending")
        val DIRECTORIES_FIRST = booleanPreferencesKey("directories_first")
        val CACHE_EXPIRE_MINUTES = intPreferencesKey("cache_expire_minutes")

        // 缓存设置
        val THUMBNAIL_CACHE_ENABLED = booleanPreferencesKey("thumbnail_cache_enabled")

        // 高级设置
        val GESTURE_CONTROL = booleanPreferencesKey("gesture_control")
        val TRUST_SELF_SIGNED_CERT = booleanPreferencesKey("trust_self_signed_cert")
    }

    // ============ 播放设置 ============

    /** 播放引擎类型 */
    val playerEngine: Flow<EngineType> = dataStore.data.map { prefs ->
        prefs[Keys.PLAYER_ENGINE]?.let { runCatching { EngineType.valueOf(it) }.getOrNull() }
            ?: EngineType.EXOPLAYER
    }

    /** 自动播放下一首 */
    val autoPlayNext: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.AUTO_PLAY_NEXT] ?: true
    }

    /** 默认播放速度 */
    val defaultPlaybackSpeed: Flow<Float> = dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_PLAYBACK_SPEED] ?: 1.0f
    }

    // ============ 外观设置 ============

    /** 主题模式 */
    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    /** 动态色彩开关 */
    val dynamicColor: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.DYNAMIC_COLOR] ?: true
    }

    // ============ 浏览设置 ============

    /** 默认排序字段 */
    val defaultSortField: Flow<SortField> = dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_SORT_FIELD]?.let { runCatching { SortField.valueOf(it) }.getOrNull() }
            ?: SortField.NAME
    }

    /** 默认排序方向（升序） */
    val defaultSortAscending: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_SORT_ASCENDING] ?: true
    }

    /** 目录优先 */
    val directoriesFirst: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.DIRECTORIES_FIRST] ?: true
    }

    /** 缓存过期时间（分钟） */
    val cacheExpireMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.CACHE_EXPIRE_MINUTES] ?: 5
    }

    // ============ 缓存设置 ============

    /** 缩略图缓存开关 */
    val thumbnailCacheEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.THUMBNAIL_CACHE_ENABLED] ?: true
    }

    // ============ 高级设置 ============

    /** 手势控制开关 */
    val gestureControl: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.GESTURE_CONTROL] ?: true
    }

    /** 自签证书信任开关 */
    val trustSelfSignedCert: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.TRUST_SELF_SIGNED_CERT] ?: false
    }

    // ============ 修改方法 ============

    /** 设置播放引擎 */
    suspend fun setPlayerEngine(engine: EngineType) {
        dataStore.edit { it[Keys.PLAYER_ENGINE] = engine.name }
    }

    /** 设置自动播放下一首 */
    suspend fun setAutoPlayNext(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
    }

    /** 设置默认播放速度 */
    suspend fun setDefaultPlaybackSpeed(speed: Float) {
        dataStore.edit { it[Keys.DEFAULT_PLAYBACK_SPEED] = speed }
    }

    /** 设置主题模式 */
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    /** 设置动态色彩开关 */
    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    /** 设置默认排序字段 */
    suspend fun setDefaultSortField(field: SortField) {
        dataStore.edit { it[Keys.DEFAULT_SORT_FIELD] = field.name }
    }

    /** 设置默认排序方向 */
    suspend fun setDefaultSortAscending(ascending: Boolean) {
        dataStore.edit { it[Keys.DEFAULT_SORT_ASCENDING] = ascending }
    }

    /** 设置目录优先 */
    suspend fun setDirectoriesFirst(enabled: Boolean) {
        dataStore.edit { it[Keys.DIRECTORIES_FIRST] = enabled }
    }

    /** 设置缓存过期时间 */
    suspend fun setCacheExpireMinutes(minutes: Int) {
        dataStore.edit { it[Keys.CACHE_EXPIRE_MINUTES] = minutes }
    }

    /** 设置缩略图缓存开关 */
    suspend fun setThumbnailCacheEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.THUMBNAIL_CACHE_ENABLED] = enabled }
    }

    /** 设置手势控制开关 */
    suspend fun setGestureControl(enabled: Boolean) {
        dataStore.edit { it[Keys.GESTURE_CONTROL] = enabled }
    }

    /** 设置自签证书信任开关 */
    suspend fun setTrustSelfSignedCert(enabled: Boolean) {
        dataStore.edit { it[Keys.TRUST_SELF_SIGNED_CERT] = enabled }
    }
}

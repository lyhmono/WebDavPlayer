package com.webdav.player.core.cache

import com.webdav.player.core.database.DirectoryCacheDao
import com.webdav.player.core.database.entity.DirectoryCacheEntity
import com.webdav.player.data.model.WebDavEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 目录缓存管理器
 *
 * 策略：先返回缓存 → 后台刷新
 * 缓存过期判断（默认 5 分钟）
 */
@Singleton
class DirectoryCacheManager @Inject constructor(
    private val directoryCacheDao: DirectoryCacheDao
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val entrySerializer = WebDavEntrySerializer

    companion object {
        /** 缓存有效期：5 分钟 */
        const val CACHE_EXPIRY_MS = 5 * 60 * 1000L
    }

    /**
     * 获取缓存的目录内容
     *
     * @param serverId 服务器 ID
     * @param path 目录路径
     * @return 缓存的条目列表，如果无缓存或已过期返回 null
     */
    suspend fun getCached(serverId: Long, path: String): List<WebDavEntry>? {
        return withContext(Dispatchers.IO) {
            val entity = directoryCacheDao.get(serverId, normalizePath(path)) ?: return@withContext null

            // 检查是否过期
            val isExpired = System.currentTimeMillis() - entity.cachedAt > CACHE_EXPIRY_MS
            if (isExpired) return@withContext null

            try {
                json.decodeFromString(entrySerializer, entity.contentJson)
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * 获取缓存的目录内容（即使已过期也返回，用于离线时显示旧数据）
     *
     * @param serverId 服务器 ID
     * @param path 目录路径
     * @return 缓存的条目列表，如果无缓存返回 null
     */
    suspend fun getCachedEvenIfExpired(serverId: Long, path: String): List<WebDavEntry>? {
        return withContext(Dispatchers.IO) {
            val entity = directoryCacheDao.get(serverId, normalizePath(path)) ?: return@withContext null

            try {
                json.decodeFromString(entrySerializer, entity.contentJson)
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * 刷新缓存
     *
     * @param serverId 服务器 ID
     * @param path 目录路径
     * @param entries 目录条目列表
     */
    suspend fun refresh(serverId: Long, path: String, entries: List<WebDavEntry>) {
        withContext(Dispatchers.IO) {
            val contentJson = json.encodeToString(entrySerializer, entries)
            val entity = DirectoryCacheEntity(
                serverId = serverId,
                path = normalizePath(path),
                contentJson = contentJson,
                cachedAt = System.currentTimeMillis()
            )
            directoryCacheDao.insert(entity)
        }
    }

    /**
     * 清除指定服务器的所有缓存
     */
    suspend fun clearByServer(serverId: Long) {
        withContext(Dispatchers.IO) {
            directoryCacheDao.deleteByServer(serverId)
        }
    }

    /**
     * 清除指定目录的缓存
     */
    suspend fun clear(serverId: Long, path: String) {
        withContext(Dispatchers.IO) {
            directoryCacheDao.delete(serverId, normalizePath(path))
        }
    }

    /**
     * 清除所有缓存
     */
    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            directoryCacheDao.clearAll()
        }
    }

    /**
     * 清除过期缓存
     */
    suspend fun clearExpired() {
        withContext(Dispatchers.IO) {
            directoryCacheDao.deleteOlderThan(System.currentTimeMillis() - CACHE_EXPIRY_MS)
        }
    }

    private fun normalizePath(path: String): String {
        var p = path.trim()
        if (!p.startsWith("/")) p = "/$p"
        return p
    }
}

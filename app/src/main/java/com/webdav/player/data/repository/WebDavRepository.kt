package com.webdav.player.data.repository

import com.webdav.player.core.webdav.WebDavClient
import com.webdav.player.core.webdav.WebDavEntry
import com.webdav.player.data.model.ServerConfig
import com.webdav.player.data.model.WebDavEntry as DomainWebDavEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebDAV 仓库
 */
@Singleton
class WebDavRepository @Inject constructor(
    private val okHttpClient: okhttp3.OkHttpClient
) {
    /** 缓存的 WebDavClient 实例，按服务器 ID */
    private val clientCache = mutableMapOf<Long, WebDavClient>()

    /**
     * 创建或获取 WebDavClient
     */
    fun getClient(serverConfig: ServerConfig): WebDavClient {
        return clientCache.getOrPut(serverConfig.id) {
            WebDavClient(
                httpClient = okHttpClient,
                baseUrl = serverConfig.url,
                username = serverConfig.username,
                password = serverConfig.password
            )
        }
    }

    /**
     * 测试连接
     */
    suspend fun testConnection(serverConfig: ServerConfig): Result<Unit> {
        return try {
            val client = getClient(serverConfig)
            client.testConnection()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 列出目录内容
     */
    suspend fun listDirectory(
        serverConfig: ServerConfig,
        path: String
    ): Result<List<DomainWebDavEntry>> {
        return try {
            val client = getClient(serverConfig)
            val entries = client.listDirectory(path)
            val domainEntries = entries.map { it.toDomain() }
            Result.success(domainEntries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 清除客户端缓存（服务器配置变更时调用）
     */
    fun clearClientCache(serverId: Long) {
        clientCache.remove(serverId)
    }

    /**
     * 清除所有缓存
     */
    fun clearAllCache() {
        clientCache.clear()
    }

    private fun WebDavEntry.toDomain(): DomainWebDavEntry {
        return DomainWebDavEntry(
            path = path,
            displayName = displayName,
            isDirectory = isDirectory,
            size = size,
            lastModified = lastModified,
            contentType = contentType,
            etag = etag
        )
    }
}

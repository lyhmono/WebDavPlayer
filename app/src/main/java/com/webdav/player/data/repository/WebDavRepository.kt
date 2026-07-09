package com.webdav.player.data.repository

import com.webdav.player.core.webdav.WebDavClient
import com.webdav.player.core.webdav.WebDavEntry
import com.webdav.player.data.model.ServerConfig
import com.webdav.player.data.model.WebDavEntry as DomainWebDavEntry
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.InputStream
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
     * 上传文件
     *
     * @param serverConfig 服务器配置
     * @param path 目标路径
     * @param inputStream 文件输入流
     * @param contentType MIME 类型
     * @param onProgress 进度回调（0.0 ~ 1.0）
     */
    suspend fun uploadFile(
        serverConfig: ServerConfig,
        path: String,
        inputStream: InputStream,
        contentType: String = "application/octet-stream",
        onProgress: ((Float) -> Unit)? = null
    ): Result<Unit> {
        return try {
            val client = getClient(serverConfig)
            // 对于带进度跟踪的上传，先将流写入临时文件
            val tempFile = File.createTempFile("upload_", ".tmp")
            tempFile.deleteOnExit()

            var totalBytes = 0L
            tempFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                }
            }
            inputStream.close()

            // 使用带进度的上传
            val requestBody = tempFile.asRequestBody(contentType.toMediaType())
            val progressBody = ProgressRequestBody(requestBody) { sent, total ->
                onProgress?.invoke(if (total > 0) sent.toFloat() / total.toFloat() else 0f)
            }

            client.uploadFile(path, progressBody)
            tempFile.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除文件
     */
    suspend fun deleteFile(serverConfig: ServerConfig, path: String): Result<Unit> {
        return try {
            val client = getClient(serverConfig)
            client.delete(path)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 重命名/移动文件
     */
    suspend fun renameFile(
        serverConfig: ServerConfig,
        oldPath: String,
        newPath: String
    ): Result<Unit> {
        return try {
            val client = getClient(serverConfig)
            client.move(oldPath, newPath, overwrite = false)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 创建目录
     */
    suspend fun createDirectory(serverConfig: ServerConfig, path: String): Result<Unit> {
        return try {
            val client = getClient(serverConfig)
            client.createDirectory(path)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取文件流（用于缩略图提取等）
     *
     * @param serverConfig 服务器配置
     * @param path 文件路径
     * @param byteRange 可选的字节范围
     * @return 文件输入流
     */
    suspend fun getFileStream(
        serverConfig: ServerConfig,
        path: String,
        byteRange: LongRange? = null
    ): InputStream {
        val client = getClient(serverConfig)
        return client.getFile(path, byteRange)
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

package com.webdav.player.core.cache

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.LruCache
import com.webdav.player.data.model.ServerConfig
import com.webdav.player.data.model.WebDavEntry
import com.webdav.player.data.repository.WebDavRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 缩略图缓存管理器
 *
 * - 视频缩略图：通过 WebDavClient 获取文件头部数据，用 MediaMetadataRetriever 提取帧
 * - 音频封面：预留接口（M2 先不实现）
 * - LRU 内存缓存 + 磁盘缓存
 */
@Singleton
class ThumbnailCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webDavRepository: WebDavRepository
) {
    /** 内存缓存：path -> 缩略图文件路径 */
    private val memoryCache = LruCache<String, String>(MEMORY_CACHE_SIZE)

    /** 磁盘缓存目录 */
    private val diskCacheDir: File by lazy {
        File(context.cacheDir, "thumbnails").apply {
            if (!exists()) mkdirs()
        }
    }

    companion object {
        private const val MEMORY_CACHE_SIZE = 64 // 最多缓存 64 个缩略图路径
        private const val MAX_DISK_CACHE_BYTES = 100L * 1024 * 1024 // 100MB
        private const val VIDEO_HEADER_BYTES = 2L * 1024 * 1024 // 下载前 2MB 用于提取帧
    }

    /**
     * 获取视频缩略图文件路径
     *
     * @param serverConfig 服务器配置
     * @param entry 文件条目
     * @return 缩略图文件路径，如果无法提取返回 null
     */
    suspend fun getVideoThumbnail(serverConfig: ServerConfig, entry: WebDavEntry): String? {
        val cacheKey = buildCacheKey(serverConfig.id, entry.path)

        // 1. 检查内存缓存
        memoryCache.get(cacheKey)?.let { cachedPath ->
            if (File(cachedPath).exists()) return cachedPath
            memoryCache.remove(cacheKey)
        }

        // 2. 检查磁盘缓存
        val diskFile = File(diskCacheDir, "$cacheKey.jpg")
        if (diskFile.exists()) {
            memoryCache.put(cacheKey, diskFile.absolutePath)
            return diskFile.absolutePath
        }

        // 3. 从网络提取
        return withContext(Dispatchers.IO) {
            try {
                extractVideoFrame(serverConfig, entry, diskFile)?.let { path ->
                    memoryCache.put(cacheKey, path)
                    path
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * 获取音频封面（预留接口，M2 暂不实现）
     *
     * @param serverConfig 服务器配置
     * @param entry 文件条目
     * @return 封面文件路径，M2 阶段始终返回 null
     */
    suspend fun getAudioCover(serverConfig: ServerConfig, entry: WebDavEntry): String? {
        // TODO: M3 阶段实现音频封面提取
        return null
    }

    /**
     * 提取视频帧
     *
     * 下载文件头部数据到临时文件，然后用 MediaMetadataRetriever 提取一帧
     */
    private suspend fun extractVideoFrame(
        serverConfig: ServerConfig,
        entry: WebDavEntry,
        outputFile: File
    ): String? {
        val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.dat")

        return try {
            // 下载文件头部数据
            val client = webDavRepository.getClient(serverConfig)
            val inputStream = client.getFile(entry.path, 0L until VIDEO_HEADER_BYTES)

            FileOutputStream(tempFile).use { fos ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    fos.write(buffer, 0, bytesRead)
                }
            }
            inputStream.close()

            // 使用 MediaMetadataRetriever 提取帧
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(tempFile.absolutePath)

            val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()

            if (bitmap != null) {
                FileOutputStream(outputFile).use { fos ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, fos)
                }
                bitmap.recycle()
                outputFile.absolutePath
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            tempFile.delete()
        }
    }

    /**
     * 构建缓存键
     */
    private fun buildCacheKey(serverId: Long, path: String): String {
        return "${serverId}_${path.replace("/", "_").replace("\\", "_")}"
    }

    /**
     * 清除内存缓存
     */
    fun clearMemoryCache() {
        memoryCache.evictAll()
    }

    /**
     * 清除磁盘缓存
     */
    fun clearDiskCache() {
        diskCacheDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * 清除全部缓存
     */
    fun clearAll() {
        clearMemoryCache()
        clearDiskCache()
    }

    /**
     * 清理过期的磁盘缓存（超过最大容量时删除最旧的文件）
     */
    fun trimDiskCache() {
        val files = diskCacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var totalSize = files.sumOf { it.length() }

        for (file in files) {
            if (totalSize <= MAX_DISK_CACHE_BYTES) break
            totalSize -= file.length()
            file.delete()
        }
    }
}

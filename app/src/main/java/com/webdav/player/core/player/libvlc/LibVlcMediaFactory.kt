package com.webdav.player.core.player.libvlc

import android.content.Context
import android.net.Uri
import com.webdav.player.core.player.MediaSource
import com.webdav.player.data.model.ServerConfig
import com.webdav.player.data.repository.ServerConfigRepository
import com.webdav.player.data.repository.WebDavRepository
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.interfaces.IMedia
import java.util.concurrent.TimeUnit

/**
 * LibVLC 媒体工厂
 *
 * 将 MediaSource 转换为 LibVLC Media，构建带认证的 HTTP URL
 */
class LibVlcMediaFactory(
    private val context: Context,
    private val serverConfigRepository: ServerConfigRepository,
    private val webDavRepository: WebDavRepository
) {

    /**
     * 将 MediaSource 转换为带认证的 HTTP URL
     *
     * LibVLC 原生支持 HTTP URL，通过在 URL 中嵌入认证信息实现 Basic Auth
     */
    private fun buildAuthUrl(mediaSource: MediaSource): String {
        val serverId = mediaSource.serverId.toLongOrNull()
            ?: throw IllegalStateException("无效的服务器 ID: ${mediaSource.serverId}")

        val config = serverConfigRepository.getById(serverId)
            ?: throw IllegalStateException("服务器配置不存在: ${mediaSource.serverId}")

        val baseUrl = config.url.trimEnd('/')
        val path = mediaSource.path.let { if (it.startsWith("/")) it else "/$it" }

        // 构建 URL: http://user:pass@host/path
        val encodedUsername = Uri.encode(config.username)
        val encodedPassword = Uri.encode(config.password)

        // 解析 baseUrl 提取 scheme 和 host
        val uri = Uri.parse(baseUrl)
        val scheme = uri.scheme ?: "http"
        val host = uri.host ?: throw IllegalStateException("无效的服务器 URL: $baseUrl")
        val port = if (uri.port != -1) ":${uri.port}" else ""

        return "$scheme://$encodedUsername:$encodedPassword@$host$port$path"
    }

    /**
     * 创建 LibVLC Media
     *
     * @param libVLC LibVLC 实例
     * @param mediaSource 媒体源
     * @return LibVLC Media 对象
     */
    fun createMedia(libVLC: LibVLC, mediaSource: MediaSource): Media {
        val authUrl = buildAuthUrl(mediaSource)
        val media = Media(libVLC, authUrl)

        // 设置 HTTP header（备用认证方式）
        media.addOption(":http-header=Authorization: Basic ${getBase64Auth(mediaSource)}")

        // 硬件加速
        media.addOption(":hwdec=auto")

        // 网络缓存（毫秒）
        media.addOption(":network-caching=3000")

        // 时长检测
        media.addOption(":duration-sleep=500")

        return media
    }

    /**
     * 创建 LibVLC Media 列表
     */
    fun createMediaList(libVLC: LibVLC, mediaSources: List<MediaSource>): List<Media> {
        return mediaSources.map { createMedia(libVLC, it) }
    }

    /**
     * 获取 Base64 编码的认证信息
     */
    private fun getBase64Auth(mediaSource: MediaSource): String {
        val serverId = mediaSource.serverId.toLongOrNull()
            ?: throw IllegalStateException("无效的服务器 ID: ${mediaSource.serverId}")

        val config = serverConfigRepository.getById(serverId)
            ?: throw IllegalStateException("服务器配置不存在: ${mediaSource.serverId}")

        val credentials = "${config.username}:${config.password}"
        return android.util.Base64.encodeToString(
            credentials.toByteArray(),
            android.util.Base64.NO_WRAP
        )
    }
}

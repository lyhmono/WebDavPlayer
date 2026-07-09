package com.webdav.player.core.player

/**
 * 媒体源数据类
 */
data class MediaSource(
    /** 唯一 ID */
    val id: String,
    /** 服务器 ID */
    val serverId: String,
    /** WebDAV 路径 */
    val path: String,
    /** 显示名称 */
    val displayName: String,
    /** 媒体类型 */
    val mediaType: MediaType,
    /** 文件大小（字节） */
    val size: Long,
    /** MIME 类型 */
    val mimeType: String? = null,
    /** 缩略图 URL（可选） */
    val thumbnailUrl: String? = null
) {
    /**
     * 构建播放 URI 字符串
     */
    fun toUriString(): String = "webdav://$serverId$path"
}

/**
 * 媒体类型
 */
enum class MediaType {
    VIDEO,
    AUDIO,
    UNKNOWN
}

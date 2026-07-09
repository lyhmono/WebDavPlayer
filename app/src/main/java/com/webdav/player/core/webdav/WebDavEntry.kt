package com.webdav.player.core.webdav

import kotlinx.serialization.Serializable

/**
 * WebDAV 目录条目（文件或目录）
 */
@Serializable
data class WebDavEntry(
    /** 完整路径，以 / 开头 */
    val path: String,
    /** 显示名称 */
    val displayName: String,
    /** 是否为目录 */
    val isDirectory: Boolean,
    /** 文件大小（字节），目录为 0 */
    val size: Long,
    /** 最后修改时间 (Unix 时间戳，毫秒) */
    val lastModified: Long,
    /** MIME 类型 */
    val contentType: String,
    /** ETag */
    val etag: String?
) {
    /** 文件扩展名（不含 .），目录返回空 */
    val extension: String
        get() = if (isDirectory) "" else displayName.substringAfterLast('.', "").lowercase()

    /** 是否为视频文件 */
    val isVideo: Boolean
        get() = !isDirectory && extension in VIDEO_EXTENSIONS

    /** 是否为音频文件 */
    val isAudio: Boolean
        get() = !isDirectory && extension in AUDIO_EXTENSIONS

    /** 是否为可播放的媒体文件 */
    val isPlayable: Boolean
        get() = isVideo || isAudio

    companion object {
        val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "flv", "wmv", "webm", "m4v", "ts", "mpg", "mpeg")
        val AUDIO_EXTENSIONS = setOf("mp3", "flac", "aac", "ogg", "wav", "m4a", "wma", "opus", "ape")
    }
}

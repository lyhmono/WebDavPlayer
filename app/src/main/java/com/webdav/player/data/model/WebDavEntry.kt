package com.webdav.player.data.model

/**
 * WebDAV 目录条目（领域模型）
 */
data class WebDavEntry(
    val path: String,
    val displayName: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val contentType: String,
    val etag: String?
) {
    val extension: String
        get() = if (isDirectory) "" else displayName.substringAfterLast('.', "").lowercase()

    val isVideo: Boolean
        get() = !isDirectory && extension in VIDEO_EXTENSIONS

    val isAudio: Boolean
        get() = !isDirectory && extension in AUDIO_EXTENSIONS

    val isPlayable: Boolean
        get() = isVideo || isAudio

    companion object {
        val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "flv", "wmv", "webm", "m4v", "ts", "mpg", "mpeg")
        val AUDIO_EXTENSIONS = setOf("mp3", "flac", "aac", "ogg", "wav", "m4a", "wma", "opus", "ape")
    }
}

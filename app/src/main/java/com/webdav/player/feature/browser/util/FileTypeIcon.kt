package com.webdav.player.feature.browser.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.music_note
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 文件类型图标映射
 *
 * 根据文件扩展名返回对应的 Material Icon
 */
object FileTypeIcon {

    /** 视频扩展名 */
    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "mov", "flv", "wmv", "webm", "m4v", "ts", "mpg", "mpeg"
    )

    /** 音频扩展名 */
    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "flac", "aac", "ogg", "wav", "m4a", "wma", "opus", "ape"
    )

    /** 图片扩展名 */
    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tiff", "ico"
    )

    /** 文档扩展名 */
    private val DOCUMENT_EXTENSIONS = setOf(
        "txt", "md", "doc", "docx", "rtf", "odt", "pages"
    )

    /** PDF 扩展名 */
    private val PDF_EXTENSIONS = setOf("pdf")

    /** 压缩包扩展名 */
    private val ARCHIVE_EXTENSIONS = setOf(
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso"
    )

    /**
     * 根据文件信息返回对应的图标
     *
     * @param isDirectory 是否为目录
     * @param fileName 文件名
     * @return 对应的 ImageVector
     */
    fun getIcon(isDirectory: Boolean, fileName: String): ImageVector {
        if (isDirectory) return Icons.Default.Folder

        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            ext in VIDEO_EXTENSIONS -> Icons.Default.VideoFile
            ext in AUDIO_EXTENSIONS -> Icons.Default.AudioFile
            ext in IMAGE_EXTENSIONS -> Icons.Default.Image
            ext in PDF_EXTENSIONS -> Icons.Default.PictureAsPdf
            ext in DOCUMENT_EXTENSIONS -> Icons.Default.Description
            ext in ARCHIVE_EXTENSIONS -> Icons.Default.FolderZip
            else -> Icons.Default.InsertDriveFile
        }
    }

    /**
     * 判断是否为视频文件
     */
    fun isVideo(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in VIDEO_EXTENSIONS
    }

    /**
     * 判断是否为音频文件
     */
    fun isAudio(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in AUDIO_EXTENSIONS
    }

    /**
     * 判断是否为图片文件
     */
    fun isImage(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in IMAGE_EXTENSIONS
    }
}

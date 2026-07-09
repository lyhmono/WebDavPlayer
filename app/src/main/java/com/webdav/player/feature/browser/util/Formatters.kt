package com.webdav.player.feature.browser.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ln
import kotlin.math.pow

/**
 * 格式化工具
 */
object Formatters {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 格式化文件大小
     *
     * @param bytes 字节数
     * @return 格式化的字符串，如 "1.23 MB"
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
            .coerceIn(0, units.lastIndex)

        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return String.format(Locale.US, "%.2f %s", value, units[digitGroups])
    }

    /**
     * 格式化日期时间
     *
     * @param timestamp Unix 时间戳（毫秒）
     * @return 格式化的字符串，如 "2024-01-15 14:30"
     */
    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return "未知"
        return dateFormat.format(Date(timestamp))
    }

    /**
     * 仅格式化日期
     *
     * @param timestamp Unix 时间戳（毫秒）
     * @return 格式化的字符串，如 "2024-01-15"
     */
    fun formatDateOnly(timestamp: Long): String {
        if (timestamp <= 0) return "未知"
        return dateOnlyFormat.format(Date(timestamp))
    }

    /**
     * 格式化时长
     *
     * @param ms 毫秒
     * @return 格式化的字符串，如 "1:23:45" 或 "3:45"
     */
    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "00:00"

        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * 格式化相对时间
     *
     * @param timestamp Unix 时间戳（毫秒）
     * @return 如 "刚刚"、"3 分钟前"、"2 小时前"、"昨天"、"3 天前"
     */
    fun formatRelativeTime(timestamp: Long): String {
        if (timestamp <= 0) return "未知"

        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "刚刚"
            diff < 3_600_000 -> "${diff / 60_000} 分钟前"
            diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
            diff < 172_800_000 -> "昨天"
            diff < 604_800_000 -> "${diff / 86_400_000} 天前"
            else -> formatDateOnly(timestamp)
        }
    }
}

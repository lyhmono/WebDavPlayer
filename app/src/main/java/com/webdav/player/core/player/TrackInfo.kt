package com.webdav.player.core.player

/**
 * 轨道类型
 */
enum class TrackType {
    /** 音轨 */
    AUDIO,
    /** 字幕 */
    SUBTITLE,
    /** 视频轨 */
    VIDEO
}

/**
 * 轨道信息
 *
 * 描述媒体文件中的音轨/字幕轨/视频轨
 */
data class TrackInfo(
    /** 轨道 ID（引擎内部标识） */
    val id: String,
    /** 语言标签（如 "zh"、"en"），可能为 null */
    val language: String?,
    /** 轨道标题/名称 */
    val title: String?,
    /** 轨道类型 */
    val trackType: TrackType,
    /** 是否当前选中 */
    val isSelected: Boolean
) {
    /**
     * 显示名称：优先使用标题，其次语言，最后使用 ID
     */
    val displayName: String
        get() = when {
            !title.isNullOrBlank() -> title!!
            !language.isNullOrBlank() -> language!!
            else -> id
        }
}

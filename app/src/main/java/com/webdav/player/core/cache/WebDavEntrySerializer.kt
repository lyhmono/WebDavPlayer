package com.webdav.player.core.cache

import com.webdav.player.data.model.WebDavEntry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

/**
 * WebDavEntry 的可序列化包装类
 *
 * 领域模型 WebDavEntry 未标注 @Serializable，
 * 使用此包装类进行 JSON 序列化/反序列化
 */
@Serializable
data class WebDavEntrySerializable(
    val path: String,
    val displayName: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val contentType: String,
    val etag: String? = null
) {
    fun toDomain(): WebDavEntry = WebDavEntry(
        path = path,
        displayName = displayName,
        isDirectory = isDirectory,
        size = size,
        lastModified = lastModified,
        contentType = contentType,
        etag = etag
    )

    companion object {
        fun from(entry: WebDavEntry): WebDavEntrySerializable = WebDavEntrySerializable(
            path = entry.path,
            displayName = entry.displayName,
            isDirectory = entry.isDirectory,
            size = entry.size,
            lastModified = entry.lastModified,
            contentType = entry.contentType,
            etag = entry.etag
        )
    }
}

/**
 * WebDavEntry 列表的序列化器
 */
val WebDavEntrySerializer = ListSerializer(WebDavEntrySerializable.serializer()).let { listSerializer ->
    object : KSerializer<List<WebDavEntry>> {
        override val descriptor: SerialDescriptor = listSerializer.descriptor

        override fun serialize(encoder: Encoder, value: List<WebDavEntry>) {
            val serializableList = value.map { WebDavEntrySerializable.from(it) }
            listSerializer.serialize(encoder, serializableList)
        }

        override fun deserialize(decoder: Decoder): List<WebDavEntry> {
            return listSerializer.deserialize(decoder).map { it.toDomain() }
        }
    }
}

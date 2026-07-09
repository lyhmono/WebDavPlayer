package com.webdav.player.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 服务器配置
 */
@Entity(
    tableName = "server_configs",
    indices = [Index(value = ["name"], unique = true)]
)
data class ServerConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 服务器名称 */
    val name: String,
    /** 服务器地址 */
    val url: String,
    /** 用户名 */
    val username: String,
    /** 密码（Base64 编码） */
    val password: String,
    /** 是否信任自签证书 */
    val trustAllCerts: Boolean = false,
    /** 已信任的证书指纹列表（分号分隔） */
    val trustedFingerprints: String = "",
    /** 创建时间 */
    val createdAt: Long = System.currentTimeMillis(),
    /** 最后修改时间 */
    val updatedAt: Long = System.currentTimeMillis(),
    /** 是否为默认服务器 */
    val isDefault: Boolean = false
)

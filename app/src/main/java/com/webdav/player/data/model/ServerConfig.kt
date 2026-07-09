package com.webdav.player.data.model

/**
 * 服务器配置（领域模型）
 */
data class ServerConfig(
    val id: Long = 0,
    val name: String,
    val url: String,
    val username: String,
    val password: String,
    val trustAllCerts: Boolean = false,
    val trustedFingerprints: Set<String> = emptySet(),
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

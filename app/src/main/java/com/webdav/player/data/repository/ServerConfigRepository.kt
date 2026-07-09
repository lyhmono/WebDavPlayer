package com.webdav.player.data.repository

import com.webdav.player.core.database.ServerConfigDao
import com.webdav.player.core.database.entity.ServerConfigEntity
import com.webdav.player.data.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 服务器配置仓库
 */
@Singleton
class ServerConfigRepository @Inject constructor(
    private val serverConfigDao: ServerConfigDao
) {
    fun observeAll(): Flow<List<ServerConfig>> {
        return serverConfigDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getAll(): List<ServerConfig> {
        return serverConfigDao.getAll().map { it.toDomain() }
    }

    suspend fun getById(id: Long): ServerConfig? {
        return serverConfigDao.getById(id)?.toDomain()
    }

    suspend fun getDefault(): ServerConfig? {
        return serverConfigDao.getDefault()?.toDomain()
    }

    suspend fun insert(config: ServerConfig): Long {
        return serverConfigDao.insert(config.toEntity())
    }

    suspend fun update(config: ServerConfig) {
        serverConfigDao.update(config.toEntity())
    }

    suspend fun delete(id: Long) {
        serverConfigDao.deleteById(id)
    }

    suspend fun setDefault(id: Long) {
        serverConfigDao.clearDefault()
        serverConfigDao.setDefault(id)
    }

    private fun ServerConfigEntity.toDomain(): ServerConfig {
        return ServerConfig(
            id = id,
            name = name,
            url = url,
            username = username,
            password = password,
            trustAllCerts = trustAllCerts,
            trustedFingerprints = if (trustedFingerprints.isBlank()) {
                emptySet()
            } else {
                trustedFingerprints.split(";").toSet()
            },
            isDefault = isDefault,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun ServerConfig.toEntity(): ServerConfigEntity {
        return ServerConfigEntity(
            id = id,
            name = name,
            url = url,
            username = username,
            password = password,
            trustAllCerts = trustAllCerts,
            trustedFingerprints = trustedFingerprints.joinToString(";"),
            isDefault = isDefault,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }
}

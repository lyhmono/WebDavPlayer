package com.webdav.player.core.webdav

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * 自签名证书信任管理器
 *
 * 功能：
 * 1. 首次连接时捕获自签证书，通知上层请求用户确认
 * 2. 用户确认后持久化证书指纹，后续连接自动信任
 * 3. 系统 CA 签发的证书始终信任
 *
 * 使用方式：
 * - 通过 [shouldTrust] 回调向 UI 层请求确认（suspend 函数）
 * - 已信任的指纹通过 [trustedFingerprints] 提供
 */
class SelfSignedCertManager(
    private val trustedFingerprints: Set<String>,
    private val shouldTrust: suspend (certificate: X509Certificate, fingerprint: String) -> Boolean
) : X509TrustManager {

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        // WebDAV 场景不验证客户端证书
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        if (chain == null || chain.isEmpty()) {
            throw java.security.cert.CertificateException("证书链为空")
        }

        val cert = chain[0]
        val fingerprint = getCertificateFingerprint(cert)

        // 已信任的指纹直接通过
        if (fingerprint in trustedFingerprints) {
            return
        }

        // 尝试用系统默认方式验证（通过系统 CA 签发的证书）
        try {
            val systemTrustManager = getSystemDefaultTrustManager()
            systemTrustManager?.checkServerTrusted(chain, authType)
            return // 系统 CA 验证通过
        } catch (_: Exception) {
            // 系统 CA 验证失败，可能是自签证书
        }

        // 自签证书：抛出异常，由 OkHttp 拦截器捕获后请求用户确认
        throw WebDavException.CertificateNotTrusted(
            certificateFingerprint = fingerprint
        )
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    companion object {
        /**
         * 计算证书 SHA-256 指纹
         */
        fun getCertificateFingerprint(cert: X509Certificate): String {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val derBytes = cert.encoded
            val digest = md.digest(derBytes)
            return digest.joinToString(":") { "%02X".format(it) }
        }

        /**
         * 获取系统默认 TrustManager
         */
        private fun getSystemDefaultTrustManager(): X509TrustManager? {
            return try {
                val factory = javax.net.ssl.TrustManagerFactory.getInstance(
                    javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()
                )
                factory.init(null as java.security.KeyStore?)
                factory.trustManagers
                    .filterIsInstance<X509TrustManager>()
                    .firstOrNull()
            } catch (_: Exception) {
                null
            }
        }
    }
}

package com.webdav.player.core.webdav

/**
 * WebDAV 操作异常
 */
sealed class WebDavException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** 网络连接失败 */
    class NetworkError(message: String, cause: Throwable? = null) : WebDavException(message, cause)

    /** 认证失败 (401) */
    class AuthFailed(message: String = "认证失败，请检查用户名和密码") : WebDavException(message)

    /** 路径不存在 (404) */
    class NotFound(message: String = "路径不存在") : WebDavException(message)

    /** 权限不足 (403) */
    class Forbidden(message: String = "权限不足") : WebDavException(message)

    /** 服务器错误 (5xx) */
    class ServerError(statusCode: Int, message: String) : WebDavException("服务器错误 ($statusCode): $message")

    /** 自签证书未受信任 */
    class CertificateNotTrusted(
        message: String = "服务器使用自签名证书，需要用户确认",
        val certificateFingerprint: String = ""
    ) : WebDavException(message)

    /** XML 解析失败 */
    class ParseError(message: String, cause: Throwable? = null) : WebDavException(message, cause)

    /** 未知错误 */
    class Unknown(message: String, cause: Throwable? = null) : WebDavException(message, cause)
}

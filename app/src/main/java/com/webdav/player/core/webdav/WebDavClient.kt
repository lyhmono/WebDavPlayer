package com.webdav.player.core.webdav

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.DocumentBuilderFactory

/**
 * WebDAV 客户端
 *
 * 基于 OkHttp 封装，支持：
 * - PROPFIND: 列出目录内容
 * - GET: 下载文件（支持 Range）
 * - PUT: 上传文件
 * - MOVE: 移动/重命名
 * - DELETE: 删除
 *
 * 认证方式：HTTP Basic Auth
 */
class WebDavClient(
    private val httpClient: OkHttpClient,
    private val baseUrl: String,
    private val username: String,
    private val password: String
) {
    private val authHeader: String by lazy {
        Credentials.basic(username, password)
    }

    /**
     * PROPFIND: 列出目录内容
     *
     * @param path 目录路径，以 / 开头
     * @param depth 0=仅当前, 1=一级子项
     * @return 目录条目列表（不包含自身）
     */
    fun listDirectory(path: String, depth: Int = 1): List<WebDavEntry> {
        val url = buildUrl(path)
        val propfindBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <D:propfind xmlns:D="DAV:">
                <D:prop>
                    <D:displayname/>
                    <D:resourcetype/>
                    <D:getcontentlength/>
                    <D:getlastmodified/>
                    <D:getcontenttype/>
                    <D:getetag/>
                </D:prop>
            </D:propfind>
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", propfindBody.toRequestBody(XML_MEDIA_TYPE))
            .header("Authorization", authHeader)
            .header("Depth", depth.toString())
            .header("Content-Type", "application/xml; charset=utf-8")
            .build()

        val response = executeRequest(request)

        return parsePropfindResponse(response.body?.string() ?: "", path)
    }

    /**
     * GET: 下载文件
     *
     * @param path 文件路径
     * @param byteRange 可选的字节范围 (start..end)，end 为 null 表示到文件末尾
     * @return 文件输入流
     */
    fun getFile(path: String, byteRange: LongRange? = null): InputStream {
        val url = buildUrl(path)
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", authHeader)

        if (byteRange != null) {
            val rangeHeader = if (byteRange.endInclusive == Long.MAX_VALUE) {
                "bytes=${byteRange.start}-"
            } else {
                "bytes=${byteRange.start}-${byteRange.endInclusive}"
            }
            requestBuilder.header("Range", rangeHeader)
        }

        val response = executeRequest(requestBuilder.build())
        return response.body?.byteStream() ?: throw WebDavException.NetworkError("响应体为空")
    }

    /**
     * GET: 获取文件响应（用于 ExoPlayer DataSource）
     *
     * @param path 文件路径
     * @param byteRange 可选的字节范围
     * @return OkHttp Response 对象
     */
    fun getFileResponse(path: String, byteRange: LongRange? = null): Response {
        val url = buildUrl(path)
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", authHeader)

        if (byteRange != null) {
            val rangeHeader = if (byteRange.endInclusive == Long.MAX_VALUE) {
                "bytes=${byteRange.start}-"
            } else {
                "bytes=${byteRange.start}-${byteRange.endInclusive}"
            }
            requestBuilder.header("Range", rangeHeader)
        }

        return executeRequest(requestBuilder.build())
    }

    /**
     * PUT: 上传文件
     *
     * @param path 目标路径
     * @param data 文件数据
     * @param contentType MIME 类型
     */
    fun putFile(path: String, data: ByteArray, contentType: String = "application/octet-stream") {
        val url = buildUrl(path)
        val request = Request.Builder()
            .url(url)
            .put(data.toRequestBody(contentType.toMediaType()))
            .header("Authorization", authHeader)
            .build()

        executeRequest(request).close()
    }

    /**
     * MOVE: 移动/重命名
     *
     * @param sourcePath 源路径
     * @param destPath 目标路径
     * @param overwrite 是否覆盖
     */
    fun move(sourcePath: String, destPath: String, overwrite: Boolean = false) {
        val sourceUrl = buildUrl(sourcePath)
        val destUrl = buildUrl(destPath)

        val request = Request.Builder()
            .url(sourceUrl)
            .method("MOVE", "".toRequestBody(null))
            .header("Authorization", authHeader)
            .header("Destination", destUrl)
            .header("Overwrite", if (overwrite) "T" else "F")
            .build()

        executeRequest(request).close()
    }

    /**
     * DELETE: 删除文件或目录
     *
     * @param path 路径
     */
    fun delete(path: String) {
        val url = buildUrl(path)
        val request = Request.Builder()
            .url(url)
            .delete()
            .header("Authorization", authHeader)
            .build()

        executeRequest(request).close()
    }

    /**
     * MKCOL: 创建目录
     *
     * @param path 目录路径
     */
    fun createDirectory(path: String) {
        val url = buildUrl(path)
        val request = Request.Builder()
            .url(url)
            .method("MKCOL", null)
            .header("Authorization", authHeader)
            .build()

        executeRequest(request).close()
    }

    /**
     * 测试连接
     *
     * @return 成功返回 true
     */
    fun testConnection(): Boolean {
        val url = buildUrl("/")
        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", "".toRequestBody(XML_MEDIA_TYPE))
            .header("Authorization", authHeader)
            .header("Depth", "0")
            .header("Content-Type", "application/xml; charset=utf-8")
            .build()

        executeRequest(request).close()
        return true
    }

    // ============ Private helpers ============

    private fun buildUrl(path: String): String {
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val base = baseUrl.trimEnd('/')
        return "$base$normalizedPath"
    }

    private fun executeRequest(request: Request): Response {
        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: WebDavException.CertificateNotTrusted) {
            throw e
        } catch (e: Exception) {
            throw WebDavException.NetworkError("网络请求失败: ${e.message}", e)
        }

        if (!response.isSuccessful && response.code != 207) {
            val message = response.message
            val ex = when (response.code) {
                401 -> WebDavException.AuthFailed()
                403 -> WebDavException.Forbidden()
                404 -> WebDavException.NotFound()
                in 500..599 -> WebDavException.ServerError(response.code, message)
                else -> WebDavException.Unknown("HTTP ${response.code}: $message")
            }
            response.close()
            throw ex
        }

        return response
    }

    private fun parsePropfindResponse(xml: String, requestPath: String): List<WebDavEntry> {
        if (xml.isBlank()) return emptyList()

        return try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
            }
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(xml.byteInputStream())

            val responses = doc.getElementsByTagNameNS("DAV:", "response")
            val entries = mutableListOf<WebDavEntry>()

            for (i in 0 until responses.length) {
                val responseElem = responses.item(i) as Element
                val entry = parseResponseElement(responseElem)
                if (entry != null && entry.path != normalizePath(requestPath)) {
                    entries.add(entry)
                }
            }

            entries
        } catch (e: WebDavException.CertificateNotTrusted) {
            throw e
        } catch (e: Exception) {
            throw WebDavException.ParseError("解析 PROPFIND 响应失败: ${e.message}", e)
        }
    }

    private fun parseResponseElement(element: Element): WebDavEntry? {
        // href
        val hrefElem = getFirstChildElement(element, "href") ?: return null
        val href = getTextContent(hrefElem)
        val path = decodePath(href)

        // propstat
        val propstatElem = getFirstChildElement(element, "propstat") ?: return null
        val propElem = getFirstChildElement(propstatElem, "prop") ?: return null

        // resourcetype
        val resourcetypeElem = getFirstChildElement(propElem, "resourcetype")
        val isDirectory = resourcetypeElem?.let {
            getFirstChildElement(it, "collection") != null
        } ?: false

        // displayname
        val displayName = getFirstChildElement(propElem, "displayname")?.let { getTextContent(it) }
            ?: path.trimEnd('/').substringAfterLast('/').ifEmpty { path }

        // getcontentlength
        val size = getFirstChildElement(propElem, "getcontentlength")?.let {
            getTextContent(it).toLongOrNull() ?: 0L
        } ?: 0L

        // getlastmodified
        val lastModified = getFirstChildElement(propElem, "getlastmodified")?.let {
            parseHttpDate(getTextContent(it))
        } ?: 0L

        // getcontenttype
        val contentType = getFirstChildElement(propElem, "getcontenttype")?.let { getTextContent(it) }
            ?: ""

        // getetag
        val etag = getFirstChildElement(propElem, "getetag")?.let { getTextContent(it) }

        return WebDavEntry(
            path = path,
            displayName = displayName,
            isDirectory = isDirectory,
            size = size,
            lastModified = lastModified,
            contentType = contentType,
            etag = etag
        )
    }

    private fun getFirstChildElement(parent: Element, localName: String): Element? {
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val elem = node as Element
                if (elem.localName == localName || elem.tagName.endsWith(":$localName") || elem.tagName == localName) {
                    return elem
                }
            }
        }
        return null
    }

    private fun getTextContent(element: Element): String {
        return element.textContent?.trim() ?: ""
    }

    private fun decodePath(href: String): String {
        // 解码 URL 编码的路径
        val decoded = java.net.URLDecoder.decode(href, "UTF-8")
        // 提取路径部分（去掉协议和主机）
        val pathOnly = try {
            val uri = java.net.URI(decoded)
            uri.path ?: decoded
        } catch (_: Exception) {
            decoded
        }
        return normalizePath(pathOnly)
    }

    private fun normalizePath(path: String): String {
        var p = path.trim()
        if (!p.startsWith("/")) p = "/$p"
        // 保留结尾的 / 用于标识目录
        return p
    }

    private fun parseHttpDate(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, dd-MMM-yyyy HH:mm:ss zzz",
            "EEEE, dd-MMM-yy HH:mm:ss zzz"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("GMT")
                return sdf.parse(dateStr)?.time ?: 0L
            } catch (_: Exception) {
                continue
            }
        }
        return 0L
    }

    companion object {
        private val XML_MEDIA_TYPE = "application/xml; charset=utf-8".toMediaType()
    }
}

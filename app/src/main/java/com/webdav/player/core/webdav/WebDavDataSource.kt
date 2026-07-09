package com.webdav.player.core.webdav

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpUtil
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.InputStream

/**
 * ExoPlayer DataSource 实现，通过 WebDavClient 读取媒体文件
 *
 * 支持：
 * - HTTP Range 请求
 * - Basic Auth
 * - 自定义 URI scheme: webdav://serverId/path/to/file
 */
@OptIn(UnstableApi::class)
class WebDavDataSource(
    private val webDavClientProvider: (serverId: String) -> WebDavClient,
    private val okHttpClient: OkHttpClient
) : BaseDataSource(true) {

    private var inputStream: InputStream? = null
    private var response: Response? = null
    private var bytesRemaining: Long = 0L
    private var bytesTransferred: Long = 0L
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)

        // 解析 URI: webdav://serverId/path/to/file
        val uri = dataSpec.uri
        val serverId = uri.host ?: throw WebDavException.NetworkError("无效的 URI: 缺少 serverId")
        val path = uri.path ?: throw WebDavException.NetworkError("无效的 URI: 缺少路径")

        val webDavClient = webDavClientProvider(serverId)

        val byteRange = if (dataSpec.position > 0 || dataSpec.length != C.LENGTH_UNSET.toLong()) {
            val start = dataSpec.position
            val end = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
                Long.MAX_VALUE
            } else {
                start + dataSpec.length - 1
            }
            start..end
        } else {
            null
        }

        val resp = webDavClient.getFileResponse(path, byteRange)
        response = resp

        val body = resp.body ?: throw WebDavException.NetworkError("响应体为空")
        inputStream = body.byteStream()

        // 计算剩余字节
        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            val contentLength = resp.header("Content-Length")?.toLongOrNull() ?: -1L
            val totalBytes = HttpUtil.getDocumentSize(resp.header("Content-Range"))
            if (totalBytes != C.LENGTH_UNSET.toLong()) {
                totalBytes - dataSpec.position
            } else if (contentLength != -1L) {
                contentLength
            } else {
                C.LENGTH_UNSET.toLong()
            }
        }

        bytesTransferred = 0L
        opened = true
        transferStarted(dataSpec)

        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining == 0L) {
            return C.RESULT_END_OF_INPUT
        }

        val readLength = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            length
        } else {
            minOf(length, bytesRemaining.toInt())
        }

        val stream = inputStream ?: return C.RESULT_END_OF_INPUT
        val bytesRead = stream.read(buffer, offset, readLength)

        if (bytesRead == -1) {
            if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                throw WebDavException.NetworkError("意外的流结束")
            }
            return C.RESULT_END_OF_INPUT
        }

        bytesTransferred += bytesRead
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }

        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? {
        return response?.request?.url?.let { Uri.parse(it.toString()) }
    }

    override fun close() {
        try {
            inputStream?.close()
        } catch (_: Exception) {
            // ignore
        }
        inputStream = null
        response?.close()
        response = null
        opened = false
    }

    companion object {
        const val SCHEME = "webdav"

        /**
         * 构建 WebDAV URI
         */
        fun buildUri(serverId: String, path: String): Uri {
            return Uri.parse("$SCHEME://$serverId$path")
        }
    }
}

/**
 * WebDavDataSource 工厂
 */
@OptIn(UnstableApi::class)
class WebDavDataSourceFactory(
    private val webDavClientProvider: (serverId: String) -> WebDavClient,
    private val okHttpClient: OkHttpClient
) : DataSource.Factory {
    override fun createDataSource(): WebDavDataSource {
        return WebDavDataSource(webDavClientProvider, okHttpClient)
    }
}

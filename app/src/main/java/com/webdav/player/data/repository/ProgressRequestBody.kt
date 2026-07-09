package com.webdav.player.data.repository

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.buffer

/**
 * 带进度跟踪的 RequestBody
 *
 * 包装原始 RequestBody，在写入时回调进度
 */
class ProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (sentBytes: Long, totalBytes: Long) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {\n        val totalBytes = contentLength()\n        var sentBytes = 0L\n\n        val countingSink = object : okio.ForwardingSink(sink) {\n            override fun write(source: okio.Buffer, byteCount: Long) {\n                super.write(source, byteCount)\n                sentBytes += byteCount\n                onProgress(sentBytes, totalBytes)\n            }\n        }\n\n        val bufferedSink = countingSink.buffer()\n        delegate.writeTo(bufferedSink)\n        bufferedSink.flush()\n    }
}

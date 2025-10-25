package http.request

import http.createByteStream
import java.nio.channels.AsynchronousSocketChannel

class RequestFactory {
    private val startlineBuilder = StartLineBuilder()
    private val headersBuilder = HeadersBuilder()
    private val bodyBuilder = BodyBuilder()

    suspend fun createRequest(channel: AsynchronousSocketChannel): HttpRequest {
        val byteStream = createByteStream(channel)
        val startLine = startlineBuilder.build(byteStream)
        val headers = headersBuilder.build(byteStream)

        val contentLength = headers.firstOrNull { it.name == "content-length" }?.value?.toIntOrNull() ?: 0
        val body = if (contentLength > 0) bodyBuilder.build(byteStream, contentLength) else null

        return HttpRequest(
            method = startLine.method,
            url = startLine.url,
            headers = headers,
            body = body
        )
    }
}
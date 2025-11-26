package http.request

import http.createByteStream
import java.nio.channels.AsynchronousSocketChannel

private const val BUFFER_CAPACITY = 256

class RequestFactory(private val readBufferCapacity: Int = BUFFER_CAPACITY) {
    private val startlineBuilder = StartLineBuilder()
    private val headersBuilder = HeadersBuilder()
    private val bodyBuilder = BodyBuilder()

    suspend fun createRequest(channel: AsynchronousSocketChannel): HttpRequest {
        val byteStream = createByteStream(channel, readBufferCapacity = readBufferCapacity)
        val startLine = startlineBuilder.build(byteStream)
        val headers = headersBuilder.build(byteStream)

        val contentLength = headers.firstOrNull { it.name == "content-length" }?.value?.toIntOrNull() ?: 0
        val body = bodyBuilder.build(byteStream, contentLength)

        return HttpRequest(
            method = startLine.method,
            url = startLine.url,
            headers = headers,
            body = body
        )
    }
}
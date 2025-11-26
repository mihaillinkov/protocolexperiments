package http.request

import http.ByteStream

class BodyBuilder {
    suspend fun build(byteStream: ByteStream, contentLength: Int): ByteArray {
        return byteStream.next(contentLength)
    }
}
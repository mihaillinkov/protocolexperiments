package http.request

import http.ByteStream

class BodyBuilder {
    suspend fun build(byteStream: ByteStream, contentLength: Int): ByteArray {
        return ByteArray(contentLength) { byteStream.next() }
    }
}
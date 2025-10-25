package http.request

import http.ByteStream

class BodyBuilder {
    suspend fun build(byteStream: ByteStream, contentLength: Int): ByteArray {
        return (0 until contentLength)
            .map { byteStream.next() }
            .toByteArray()
    }
}
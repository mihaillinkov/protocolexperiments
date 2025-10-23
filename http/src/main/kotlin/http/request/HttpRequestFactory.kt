package http.request

import http.HttpRequest

interface HttpRequestFactory {
    suspend fun create(byteStream: ByteStream): HttpRequest
}
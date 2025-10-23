package http

import http.request.Header

data class HttpRequest(
    val method: RequestMethod,
    val url: String,
    val headers: List<Header> = listOf(),
    val body: ByteArray? = null
)

enum class RequestMethod {
    GET, POST, PUT, DELETE
}

class BadRequest(message: String): RuntimeException(message)

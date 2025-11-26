package http.request

data class HttpRequest(
    val method: RequestMethod,
    val url: String,
    val headers: List<Header> = listOf(),
    val body: ByteArray
)

enum class RequestMethod {
    GET, POST, PUT, DELETE
}

class BadRequest(message: String): RuntimeException(message)

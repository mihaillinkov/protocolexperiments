package http

data class HttpResponse(
    val status: ResponseStatus,
    val body: String? = null,
    val headers: List<String> = emptyList()
)

data class ResponseStatus(val code: Int, val message: String = "")

fun buildResponse(response: HttpResponse) =
    "${buildResponseFirstLine(response)}\n${response.headers.joinToString("\n")}\n${response.body}".toByteArray()

fun buildResponseFirstLine(response: HttpResponse): String {
    return "HTTP/1.1 ${response.status.code} ${response.status.message}\nContent-Length: 0\n"
}

object ResponseCode {
    const val OK = 200
    const val NOT_FOUND = 404
    const val SERVER_ERROR = 500
}

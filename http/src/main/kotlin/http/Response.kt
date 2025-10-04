package http

import http.ResponseCode.REQUEST_TIMEOUT

private const val PROTOCOL_VERSION = "HTTP/1.1"

data class HttpResponse(
    val status: ResponseStatus,
    val body: String? = null,
    val headers: List<String> = emptyList()
)

data class ResponseStatus(val code: Int, val message: String = "")

fun buildHttpResponse(response: HttpResponse): ByteArray {
    val headers = response.headers.joinToString("\n")
    return """${buildResponseFirstLine(response)}
        |$headers
        |
        |${response.body}
    """.trimMargin().toByteArray()
}

fun timeoutResponse(): HttpResponse {
    return HttpResponse(
        status = ResponseStatus(REQUEST_TIMEOUT, "Request Timeout"),
        headers = listOf("Connection: close"))
}

fun buildResponseFirstLine(response: HttpResponse): String {
    return "$PROTOCOL_VERSION ${response.status.code} ${response.status.message}"
}

object ResponseCode {
    const val OK = 200
    const val NOT_FOUND = 404
    const val REQUEST_TIMEOUT = 408
    const val SERVER_ERROR = 500
}

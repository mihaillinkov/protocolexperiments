package http

private const val PROTOCOL_VERSION = "HTTP/1.1"
private const val CONTENT_LENGTH_HEADER = "Content-Length"
private const val CONNECTION_CLOSE_HEADER = "Connection: Close"
private const val CONTENT_TYPE_HEADER = "Content-type: text/plain; charset=utf-8"
private const val LINE_BREAK = "\r\n"

object ResponseCode {
    const val OK = 200
    const val BAD_REQUEST = 400
    const val NOT_FOUND = 404
    const val REQUEST_TIMEOUT = 408
    const val SERVER_ERROR = 500
}

data class HttpResponse(
    val status: ResponseStatus,
    val body: ByteArray? = null,
    val headers: List<String> = emptyList()
)

data class ResponseStatus(val code: Int, val message: String? = null) {
    companion object {
        fun ok(message: String = "OK") = ResponseStatus(ResponseCode.OK, message)
        fun notFound(message: String = "NOT_FOUND") = ResponseStatus(ResponseCode.NOT_FOUND, message)
    }
}

fun buildHttpResponse(response: HttpResponse): ByteArray {
    val commonHeaders = listOf(
        contentLengthHeader(response.body?.size ?: 0),
        CONTENT_TYPE_HEADER,
        CONNECTION_CLOSE_HEADER)

    val headers = (response.headers + commonHeaders).joinToString(LINE_BREAK)

    return ("${buildResponseStartLine(response.status)}$LINE_BREAK" +
            "$headers$LINE_BREAK$LINE_BREAK").toByteArray() +
            (response.body ?: byteArrayOf())

}

fun timeoutResponse(): HttpResponse {
    return HttpResponse(status = ResponseStatus(ResponseCode.REQUEST_TIMEOUT, "Request Timeout"))
}

fun buildResponseStartLine(status: ResponseStatus): String {
    return "$PROTOCOL_VERSION ${status.code}${status.message?.let { " $it" } ?: ""}"
}

fun contentLengthHeader(contentLength: Int): String {
    return "$CONTENT_LENGTH_HEADER: $contentLength"
}

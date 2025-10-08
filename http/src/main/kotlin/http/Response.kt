package http

import http.ResponseCode.REQUEST_TIMEOUT

private const val PROTOCOL_VERSION = "HTTP/1.1"
private const val CONTENT_LENGTH_HEADER = "Content-Length"
private const val CONNECTION_CLOSE_HEADER = "Connection: Close"
private const val CONTENT_TYPE_HEADER = "Content-type: text/plain; charset=utf-8"
private const val LINE_BREAK = "\r\n"

data class HttpResponse(
    val status: ResponseStatus,
    val body: String? = null,
    val headers: List<String> = emptyList()
)

data class ResponseStatus(val code: Int, val message: String?)

fun buildHttpResponse(response: HttpResponse): ByteArray {
    val commonHeaders = listOf(
        contentLengthHeader(calculateContentLength(response.body)),
        CONTENT_TYPE_HEADER,
        CONNECTION_CLOSE_HEADER)

    val headers = (response.headers + commonHeaders).joinToString(LINE_BREAK)

    return "${buildResponseFirstLine(response)}$LINE_BREAK$headers$LINE_BREAK$LINE_BREAK${response.body ?: ""}"
        .toByteArray()
}

fun timeoutResponse(): HttpResponse {
    return HttpResponse(status = ResponseStatus(REQUEST_TIMEOUT, "Request Timeout"))
}

fun buildResponseFirstLine(response: HttpResponse): String {
    return "$PROTOCOL_VERSION ${response.status.code} ${response.status.message ?: ""}"
}

object ResponseCode {
    const val OK = 200
    const val BAD_REQUEST = 400
    const val NOT_FOUND = 404
    const val REQUEST_TIMEOUT = 408
    const val SERVER_ERROR = 500
}

fun contentLengthHeader(contentLength: Int): String {
    return "$CONTENT_LENGTH_HEADER: $contentLength"
}

fun calculateContentLength(body: String?): Int {
    return body?.toByteArray(Charsets.UTF_8)?.size ?: 0
}

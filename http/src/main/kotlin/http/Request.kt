package http

import http.utils.split
import kotlinx.coroutines.CancellationException
import java.nio.channels.AsynchronousSocketChannel

private val END_OF_LINE = "\r\n".toByteArray().toList()
private val END_OF_HEADERS = "\r\n\r\n".toByteArray().toList()

data class Request(
    val method: RequestMethod,
    val url: String,
    val headers: List<String> = listOf()
)

enum class RequestMethod {
    GET, POST, PUT, DELETE
}

suspend fun buildRequestObject(inputStream: AsynchronousSocketChannel): Request {
    val requestLines = readLines(inputStream)
    val firstLine = String(requestLines[0]).split(" ")

    val method = firstLine[0].uppercase()
    val url = firstLine[1].lowercase()

    val headers = requestLines.drop(1).map { String(it) }

    return Request(RequestMethod.valueOf(method), url, headers)
}

suspend fun readLines(inputStream: AsynchronousSocketChannel): List<ByteArray> {
    val res = mutableListOf<Byte>()

    while (true) {
        val bytes = inputStream.readAwait() ?: throw CancellationException()
        res.addAll(bytes.toList())
        if (res.takeLast(END_OF_HEADERS.size) == END_OF_HEADERS) break
    }
    return res.split(END_OF_LINE.toList()).map { it.toByteArray() }
}

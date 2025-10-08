package http

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import java.nio.channels.AsynchronousSocketChannel

private val END_OF_LINE = "\r\n".toByteArray().toList()

private const val CONTENT_LENGTH_HEADER = "content-length"

data class Request(
    val method: RequestMethod,
    val url: String,
    val headers: List<String> = listOf(),
    val body: ByteArray? = null
)

enum class RequestMethod {
    GET, POST, PUT, DELETE
}

suspend fun buildRequestObject(inputStream: AsynchronousSocketChannel): Request {
    val startLine = String(readLine(inputStream), Charsets.UTF_8).split(" ")

    val method = RequestMethod.valueOf(startLine[0].uppercase())
    val url = startLine[1].lowercase()

    val headers = flow {
        while (true) {
            emit(readLine(inputStream))
        }
    }
        .takeWhile { it.isNotEmpty() }
        .map { String(it, Charsets.UTF_8) }
        .toList()

    val contentLength = (headers
        .firstOrNull { it.lowercase().startsWith(CONTENT_LENGTH_HEADER) } ?: "$CONTENT_LENGTH_HEADER:0")
        .replace(" ", "")
        .drop(CONTENT_LENGTH_HEADER.length + 1)
        .toInt()

    val body = if (contentLength > 0) readBody(inputStream, contentLength) else null

    return Request(method, url, headers, body)
}

suspend fun readLine(inputStream: AsynchronousSocketChannel): ByteArray {
    val res = mutableListOf<Byte>()

    while (true) {
        val bytes = inputStream.readAwait() ?: throw CancellationException()
        res.addAll(bytes.toList())
        if (res.takeLast(END_OF_LINE.size) == END_OF_LINE) break
    }
    return res.dropLast(2).toByteArray()
}

suspend fun readBody(inputStream: AsynchronousSocketChannel, contentLength: Int): ByteArray {
    val res = mutableListOf<Byte>()

    while (res.size < contentLength) {
        val bytes = inputStream.readAwait(1024) ?: throw CancellationException()
        res.addAll(bytes.toList())
    }
    return res.take(contentLength).toByteArray()
}

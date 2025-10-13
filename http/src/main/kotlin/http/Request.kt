package http

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import java.nio.channels.AsynchronousSocketChannel
import kotlin.text.replace

private val END_OF_LINE = "\r\n".toByteArray().toList()

private const val CONTENT_LENGTH_HEADER = "content-length"

private val logger = LoggerFactory.getLogger("request")

data class HttpRequest(
    val method: RequestMethod,
    val url: String,
    val headers: List<String> = listOf(),
    val body: ByteArray? = null
)

enum class RequestMethod {
    GET, POST, PUT, DELETE
}

suspend fun buildRequestObject(inputStream: AsynchronousSocketChannel): HttpRequest {
    val startLine = String(readLine(inputStream), Charsets.UTF_8)
    val startLineTokens = startLine.split(" ")

    if (startLineTokens.size != 3) {
        logger.error("StartLine should have 3 tokens, actual: {}", startLine)
        throw BadRequest("Invalid startline $startLine")
    }

    val (methodRaw, urlRaw, _) = startLineTokens

    val method = getMethod(methodRaw) ?: logInvalidMethodAndThrowBadRequest(methodRaw)
    val url = urlRaw.lowercase()

    val headers = flow {
        while (true) {
            emit(readLine(inputStream))
        }
    }
        .takeWhile { it.isNotEmpty() }
        .map { String(it, Charsets.UTF_8) }
        .toList()

    val contentLength = getContentLength(headers) ?: logInvalidContentLengthHeaderAndThrowBadRequest(headers)

    val body = if (contentLength > 0) readBody(inputStream, contentLength) else null

    return HttpRequest(method, url, headers, body)
}

fun getContentLength(headers: List<String>): Int? {
    val contentLength = headers
        .map { it.split(":") }
        .filter { it.size == 2 }
        .map { it.map { token -> token.replace(" ", "") } }
        .firstOrNull { it[0].startsWith(CONTENT_LENGTH_HEADER, ignoreCase = true) }
        ?.get(1) ?: "0"

    return contentLength.toIntOrNull()
}

fun getMethod(method: String): RequestMethod? {
    return RequestMethod.entries.firstOrNull { it.name.equals(method, true) }
}

private fun logInvalidMethodAndThrowBadRequest(method: String): Nothing {
    logger.error("Unsupported method {}", method)
    throw BadRequest("Unsupported http method $method, should be one of ${RequestMethod.entries}")
}

private fun logInvalidContentLengthHeaderAndThrowBadRequest(headers: List<String>): Nothing {
    val contentLengthHeader = headers.firstOrNull { it.startsWith(CONTENT_LENGTH_HEADER, ignoreCase = true) }
    logger.error("Invalid content-length header {}", contentLengthHeader )
    throw BadRequest("Invalid content-length header $CONTENT_LENGTH_HEADER")
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

class BadRequest(message: String): RuntimeException(message)

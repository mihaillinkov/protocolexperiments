package http.request

import http.BadRequest
import http.HttpRequest
import http.RequestMethod
import org.slf4j.LoggerFactory
import java.nio.channels.AsynchronousSocketChannel
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3

private val logger = LoggerFactory.getLogger("request-builder")

private val LINE_BREAK = "\r\n".toByteArray()

suspend fun createRequest(channel: AsynchronousSocketChannel): HttpRequest {
    val byteStream = createByteStream(channel)
    val startLine = StartLineBuilder().build(byteStream)
    val headers = HeadersBuilder().build(byteStream)

    val contentLength = (headers.firstOrNull { it.name == "content-length" } ?: Header("content-length", "0"))
        .value.toInt()

    val body = if (contentLength > 0) BodyBuilder().build(byteStream, contentLength) else null

    return HttpRequest(
        method = startLine.method,
        url = startLine.url,
        headers = headers,
        body = body
    )
}

class StartLineBuilder() {
    suspend fun build(byteStream: ByteStream): HttpRequestStartLine {
        val startLine = readLine(byteStream).toString(Charsets.UTF_8)

        val tokens = startLine.split(" ")
        if (tokens.size != 3) {
            logger.error("StartLine should have 3 tokens, actual: {}", startLine)
            throw BadRequest("Invalid startline $startLine")
        }

        val (methodRaw, urlRaw, _) = tokens

        val method = getMethod(methodRaw) ?: logInvalidMethodAndThrowBadRequest(methodRaw)
        val url = urlRaw.lowercase()

        return HttpRequestStartLine(url, method)
    }

    private fun logInvalidMethodAndThrowBadRequest(method: String): Nothing {
        logger.error("Unsupported method {}", method)
        throw BadRequest("Unsupported http method $method, should be one of ${RequestMethod.entries}")
    }
}

class HeadersBuilder() {
    suspend fun build(byteStream: ByteStream): List<Header> {
        var header = nextHeader(byteStream)
        val headers = mutableListOf<Header>()
        while (header != null) {
            headers.add(header)
            header = nextHeader(byteStream)
        }
        return headers
    }
}

class BodyBuilder {
    suspend fun build(byteStream: ByteStream, contentLength: Int): ByteArray {
        return (0 until contentLength).map { byteStream.next() }.toByteArray()
    }
}


private suspend fun nextHeader(byteStream: ByteStream): Header? {
    val bytes = readLine(byteStream)

    if (bytes.isEmpty()) {
        return null
    }

    val header = String(bytes, Charsets.UTF_8)
    val firstSemicolonPosition = header.indexOf(":")
    if (firstSemicolonPosition == -1) {
        logger.warn("Invalid header: {}", header)
        throw IllegalArgumentException("Invalid header: $header")
    }

    return Header(
        name = header.take(firstSemicolonPosition).lowercase().trim(),
        value = header.drop(firstSemicolonPosition + 1).lowercase().trim())
}

private suspend fun readLine(byteStream: ByteStream): ByteArray {
    val bytes = mutableListOf<Byte>()

    while (bytes.size < 2 || !(bytes[bytes.lastIndex - 1] == LINE_BREAK[0] && bytes[bytes.lastIndex] == LINE_BREAK[1])) {
        bytes.add(byteStream.next())
    }

    return bytes.dropLast(2).toByteArray()
}

fun getMethod(method: String): RequestMethod? {
    return RequestMethod.entries.firstOrNull { it.name.equals(method, true) }
}
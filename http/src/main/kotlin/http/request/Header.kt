package http.request

import http.ByteStream
import http.readLine
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(HeadersBuilder::class.java)

interface HeaderResult

data class Header(
    val name: String,
    val value: String): HeaderResult

data class InvalidHeader(val header: String): HeaderResult

class HeadersBuilder() {
    suspend fun build(byteStream: ByteStream): List<Header> {
        val headers = mutableListOf<Header>()

        do {
            when (val header = nextHeader(byteStream)) {
                is Header -> headers.add(header)
                null -> break
                else -> logger.warn("Invalid header {}", header)
            }
        } while (true)

        return headers
    }
}

private suspend fun nextHeader(byteStream: ByteStream): HeaderResult? {
    val bytes = byteStream.readLine()

    if (bytes.isEmpty()) {
        return null
    }

    val header = String(bytes, Charsets.UTF_8)
    val firstSemicolonPosition = header.indexOf(":")

    if (firstSemicolonPosition == -1) {
        return InvalidHeader(header)
    }

    return Header(
        name = header.take(firstSemicolonPosition).lowercase().trim(),
        value = header.drop(firstSemicolonPosition + 1).trim())
}

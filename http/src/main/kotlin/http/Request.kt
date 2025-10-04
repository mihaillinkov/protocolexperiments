package http

import java.io.InputStream

private const val END_OF_LINE = 13

data class Request(
    val method: RequestMethod,
    val url: String
)

enum class RequestMethod {
    GET, POST, PUT, DELETE
}

fun buildRequestObject(inputStream: InputStream): Request {
    val firstLineTokens = readInputFirstLine(inputStream).toString(Charsets.UTF_8).split(" ")

    val method = firstLineTokens[0].uppercase()
    val url = firstLineTokens[1].lowercase()
    return Request(RequestMethod.valueOf(method), url)
}

fun readInputFirstLine(inputStream: InputStream): ByteArray {
    val res = mutableListOf<Byte>()
    val bufferedStream = inputStream.buffered()
    while (true) {
        val byte = bufferedStream.read()
        if (byte == END_OF_LINE) break

        res.add(byte.toByte())
    }
    return res.toByteArray()
}
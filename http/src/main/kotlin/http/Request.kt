package http

import java.io.InputStream

private val END_OF_LINE = listOf(13, 10)

data class Request(
    val method: RequestMethod,
    val url: String,
    val headers: List<String> = listOf()
)

enum class RequestMethod {
    GET, POST, PUT, DELETE
}

fun buildRequestObject(inputStream: InputStream): Request {
    val requestLines = readLines(inputStream)
    val firstLine = String(requestLines[0]).split(" ")

    val method = firstLine[0].uppercase()
    val url = firstLine[1].lowercase()

    val headers = requestLines.drop(1).map { String(it) }

    return Request(RequestMethod.valueOf(method), url, headers)
}

fun readLines(inputStream: InputStream): List<ByteArray> {
    val res = mutableListOf<Int>()
    while (true) {
        val byte = inputStream.read()
        res.add(byte)
        if (res.takeLast(END_OF_LINE.size * 2) == listOf(END_OF_LINE, END_OF_LINE).flatten()) break
    }
    return res.split(END_OF_LINE).map { it.map (Int::toByte).toByteArray() }
}

fun <T> List<T>.split(separator: List<Int>): List<List<T>> {
    val res = mutableListOf<List<T>>()

    var current = mutableListOf<T>()

    for (v in this) {
        current.add(v)
        if (current.takeLast(separator.size) == separator) {
            res.add(current.dropLast(separator.size))
            current = mutableListOf()
        }
    }
    if (current.isNotEmpty()) res.add(current)
    return res
}
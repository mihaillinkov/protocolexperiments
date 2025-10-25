package http.request

import http.ByteStream
import http.readLine
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(StartLineBuilder::class.java)

class StartLineBuilder() {
    suspend fun build(byteStream: ByteStream): HttpRequestStartLine {
        val startLine = byteStream.readLine().toString(Charsets.UTF_8)

        val tokens = startLine.split(" ")
        if (tokens.size != 3) {
            logger.error("StartLine should have 3 tokens, actual: {}", startLine)
            throw BadRequest("Invalid startline $startLine")
        }

        val (methodRaw, urlRaw, _) = tokens

        val method = getMethod(methodRaw) ?: logInvalidMethodAndThrowBadRequest(methodRaw)

        return HttpRequestStartLine(urlRaw.lowercase(), method)
    }

    private fun logInvalidMethodAndThrowBadRequest(method: String): Nothing {
        logger.error("Unsupported method {}", method)
        throw BadRequest("Unsupported http method $method, should be one of ${RequestMethod.entries}")
    }
}

fun getMethod(method: String): RequestMethod? {
    return RequestMethod.entries.firstOrNull { it.name.equals(method, true) }
}
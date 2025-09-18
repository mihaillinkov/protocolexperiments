package http

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.time.measureTime


const val DEFAULT_PORT = 8080
const val END_OF_LINE = 13

const val PROCESSOR_NUMBER = 2

val logger = LoggerFactory.getLogger("main")

fun main(vararg args: String) = runBlocking {
    val port = args.getOrNull(0)?.toInt() ?: DEFAULT_PORT

    var requestCounter = 0

    val server = ServerSocket(port)
    val channel = Channel<Pair<Socket, Long>>(PROCESSOR_NUMBER)

    repeat(PROCESSOR_NUMBER) {
        launch {
            for ((socket, receivedAt) in channel) {
                val requestId = ++requestCounter
                val processingDuration = measureTime {
                    processSocket(socket)
                }
                logger.info(
                    "Request#{} complete, Processing took {} microseconds, totalTime {}",
                    requestId,
                    processingDuration.inWholeMilliseconds,
                    System.currentTimeMillis() - receivedAt
                )
            }
        }
    }

    withContext(Dispatchers.IO) {
        while (true) {
            val socket = server.accept()
            val receivedAt = System.currentTimeMillis()
            channel.send(socket to receivedAt)
        }
    }
}

suspend fun processSocket(socket: Socket) = supervisorScope {
    socket.use { socket ->
        val request = buildRequestObject(socket.getInputStream())
        logger.info("Processing request: $request")
        delay(10000)
        val response = try {
             processRequest(request)
        } catch (e: Exception) {
            logger.error("Exception while processing request", e)
            HttpResponse(ResponseStatus(500, "SOME_ERROR"))
        }
        socket.getOutputStream().write(buildResponse(response))
    }
}

data class HttpResponse(
    val status: ResponseStatus,
    val body: String? = null,
    val headers: List<String> = emptyList()
)

data class Request(
    val method: RequestMethod,
    val url: String
)

enum class RequestMethod {
    GET, POST, PUT, DELETE
}

fun buildResponse(response: HttpResponse) =
    "${buildResponseFirstLine(response)}\n${response.headers.joinToString("\n")}\n${response.body}".toByteArray()

fun buildResponseFirstLine(response: HttpResponse): String {
    return "HTTP/1.1 ${response.status.status} ${response.status.statusMessage}\nContent-Length: 0\n"
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

data class ResponseStatus(val status: Int, val statusMessage: String = "")

fun processRequest(request: Request): HttpResponse {
    val status = when (request.method) {
        RequestMethod.GET -> ResponseStatus(200, "OK")
        else -> ResponseStatus(404, "NOT_FOUND")
    }

    return HttpResponse(status)
}
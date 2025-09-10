package http

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.time.measureTime


const val DEFAULT_PORT = 8080
const val INPUT_PARALLELISM = 16
const val END_OF_LINE = 13

val logger = LoggerFactory.getLogger("main")

val requestProcessorIoDispatcher = Dispatchers.IO.limitedParallelism(INPUT_PARALLELISM)


fun main(vararg args: String) = runBlocking {
    val port = args.getOrNull(0)?.toInt() ?: DEFAULT_PORT

    val server = ServerSocket(port)
    var requestCounter = 0

    val channel = Channel<Socket>()

    launch {
        while (true) {
            val socket = channel.receive()
            launch(requestProcessorIoDispatcher) {
                val requestId = requestCounter++
                val processingDuration = measureTime {
                    processSocket(socket)
                }
                logger.info("Request#{} complete, took {} microseconds", requestId, processingDuration.inWholeMilliseconds)
            }
        }
    }

    withContext(Dispatchers.IO) {
        while (true) {
            channel.send(server.accept())
        }
    }
}

suspend fun processSocket(socket: Socket) = supervisorScope {
    socket.use { socket ->
        val request = buildRequestObject(socket.getInputStream())

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
    return "HTTP/1.1 ${response.status.status} ${response.status.statusMessage}"
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
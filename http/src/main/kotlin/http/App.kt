package http

import http.ResponseCode.NOT_FOUND
import http.ResponseCode.OK
import http.ResponseCode.SERVER_ERROR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import java.net.ServerSocket
import java.net.Socket
import java.time.Instant
import kotlin.collections.listOf
import kotlin.time.Duration
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.measureTime


private val logger = LoggerFactory.getLogger(App::class.java)

private val ARTIFICIAL_DELAY = Duration.parse("1s")

private val INPUT_READER_DISPATCHER = Dispatchers.IO.limitedParallelism(256)

class App(private val config: Config) {
    @OptIn(ExperimentalAtomicApi::class)
    suspend fun run() = coroutineScope {
        val requestCounter = AtomicInt(0)

        val server = ServerSocket(config.port)
        logger.info("Application started on port {}", config.port)

        val channel = Channel<Pair<Socket, Long>>()

        repeat(config.parallelProcessors) {
            launch(Dispatchers.IO) {
                for ((socket, receivedAt) in channel) {
                    launch {
                        val requestId = requestCounter.incrementAndFetch()
                        val processingDuration = measureTime {
                            processSocket(socket, config.requestTimeoutMs)
                        }
                        logger.info(
                            "Request#{} complete, Processing took {} milliseconds, totalTime {}, received at {}",
                            requestId,
                            processingDuration.inWholeMilliseconds,
                            System.currentTimeMillis() - receivedAt,
                            Instant.ofEpochMilli(receivedAt)
                        )
                    }
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
}

suspend fun processSocket(socket: Socket, requestTimeout: Long) {
    socket.use { socket ->
        val response = withTimeoutOrNull(requestTimeout) {
            val request = async(INPUT_READER_DISPATCHER) { buildRequestObject(socket.getInputStream()) }
                .await()
            logger.info("Processing request: $request")
            delay(ARTIFICIAL_DELAY)
            try {
                processRequest(request)
            } catch (e: Exception) {
                logger.error("Exception while processing request", e)
                HttpResponse(ResponseStatus(SERVER_ERROR, "SOME_ERROR"))
            }
        }

        val responseBytes = buildHttpResponse(response ?: timeoutResponse())
        socket.getOutputStream().write(responseBytes)
    }
}

fun processRequest(request: Request): HttpResponse {
    val status = when (request.method) {
        RequestMethod.GET -> ResponseStatus(OK, "OK")
        else -> ResponseStatus(NOT_FOUND, "NOT_FOUND")
    }

    val body = "Hello \uD83D\uDC27"

    return HttpResponse(
        status = status,
        body = body,
        headers = listOf("Content-Length: ${body.toByteArray().size}", "hello: world"))
}

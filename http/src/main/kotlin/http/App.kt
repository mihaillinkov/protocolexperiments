package http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.net.ServerSocket
import java.net.Socket
import kotlin.time.Duration
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.measureTime


private val logger = LoggerFactory.getLogger(App::class.java)

private val DELAY = Duration.parse("2s")

class App {
    companion object {
        @OptIn(ExperimentalAtomicApi::class)
        suspend fun run(config: Config) = coroutineScope {
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
}

suspend fun processSocket(socket: Socket) = supervisorScope {
    socket.use { socket ->
        val request = buildRequestObject(socket.getInputStream())
        logger.info("Processing request: $request")
        delay(DELAY)
        val response = try {
            processRequest(request)
        } catch (e: Exception) {
            logger.error("Exception while processing request", e)
            HttpResponse(ResponseStatus(ResponseCode.SERVER_ERROR, "SOME_ERROR"))
        }
        socket.getOutputStream().write(buildResponse(response))
    }
}

fun processRequest(request: Request): HttpResponse {
    val status = when (request.method) {
        RequestMethod.GET -> ResponseStatus(ResponseCode.OK, "OK")
        else -> ResponseStatus(ResponseCode.NOT_FOUND, "NOT_FOUND")
    }

    return HttpResponse(status)
}

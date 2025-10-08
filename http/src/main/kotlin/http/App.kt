package http

import http.ResponseCode.BAD_REQUEST
import http.ResponseCode.NOT_FOUND
import http.ResponseCode.OK
import http.ResponseCode.SERVER_ERROR
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.time.Instant
import kotlin.collections.listOf
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.measureTime


private val logger = LoggerFactory.getLogger(App::class.java)

class App(private val config: Config) {
    @OptIn(ExperimentalAtomicApi::class)
    suspend fun run() = coroutineScope {
        val requestCounter = AtomicInt(0)

        val serverChannel = AsynchronousServerSocketChannel.open()
            .bind(InetSocketAddress(config.port))
        logger.info("Application started on port {}", config.port)

        val channel = Channel<RequestContext>()
        repeat(config.parallelProcessors) {
            launch(Dispatchers.IO) {
                for ((receivedAt, socket) in channel) {

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

        serverChannel.use { server ->
            while (true) {
                val socket = server.acceptAwait()
                channel.send(RequestContext(System.currentTimeMillis(), socket))
            }
        }
    }
}

suspend fun processSocket(socket: AsynchronousSocketChannel, requestTimeout: Long) {
    socket.use { socket ->
        val response = withTimeoutOrNull(requestTimeout) {
            try {
                val request = buildRequestObject(socket)
                logger.info("Processing request: $request")
                processRequest(request)
            } catch (e: CancellationException) {
                throw e
            } catch (e: BadRequest) {
                logger.error("Bad request", e)
                HttpResponse(ResponseStatus(BAD_REQUEST, e.message))
            } catch (e: Exception) {
                logger.error("Exception while processing request", e)
                HttpResponse(ResponseStatus(SERVER_ERROR, e.message))
            }
        }

        socket.writeAwait(buildHttpResponse(response ?: timeoutResponse()))
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
        headers = listOf("test-header: test-1"))
}

data class RequestContext(
    val receivedAt: Long,
    val socket: AsynchronousSocketChannel)

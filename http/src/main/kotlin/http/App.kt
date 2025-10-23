package http

import http.ResponseCode.BAD_REQUEST
import http.ResponseCode.SERVER_ERROR
import http.handler.RequestHandler
import http.request.createRequest
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
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.measureTime
import kotlin.time.measureTimedValue


private val logger = LoggerFactory.getLogger(App::class.java)
private const val SOCKET_BACKLOG_SIZE = 1024

class App(private val config: Config) {
    private val handlers = mutableMapOf<Pair<String, RequestMethod>, RequestHandler>()

    fun addHandler(path: String, method: RequestMethod, handler: RequestHandler): App {
        return this.apply {
            handlers[path.lowercase() to method] = handler
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    suspend fun start() = coroutineScope {
        val requestCounter = AtomicInt(0)
        val maxParallelRequest = maxOf(1, config.maxParallelRequest)

        val serverChannel = AsynchronousServerSocketChannel.open()
            .bind(InetSocketAddress(config.port), SOCKET_BACKLOG_SIZE)
        logger.info("Application started on port {}, maxParallelRequest: {}", config.port, maxParallelRequest)

        val channel = Channel<RequestContext>()

        repeat(maxParallelRequest) {
            launch(context = Dispatchers.Default) {
                for ((receivedAt, socket) in channel) {
                    val requestId = requestCounter.incrementAndFetch()
                    val processingDuration = measureTime {
                        processSocket(socket, config.requestTimeoutMs, handlers)
                    }
                    logger.info(
                        "Request#{} complete, Processing took {} microseconds, totalTime {}, received at {}",
                        requestId,
                        processingDuration.inWholeMicroseconds,
                        System.currentTimeMillis() - receivedAt,
                        Instant.ofEpochMilli(receivedAt),
                    )
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

private suspend fun processSocket(
    socket: AsynchronousSocketChannel,
    requestTimeout: Long,
    handlers: Map<Pair<String, RequestMethod>, RequestHandler>) {

    socket.use { socket ->
        val response = withTimeoutOrNull(requestTimeout) {
            try {
                val (request, requestBuildTime) = measureTimedValue {
                    createRequest(socket)
                }

                logger.debug("Processing request: {}", request)
                val (response, handlingTime) = measureTimedValue {
                    val handler = handlers[request.url to request.method]
                    handler?.handle(request) ?: HttpResponse(status = ResponseStatus.notFound())
                }
                logger.info("requestBuildTime: {}, handlingTime: {}", requestBuildTime, handlingTime)
                response
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
        val responseProcessingTime = measureTime {
            socket.writeAwait(buildHttpResponse(response ?: timeoutResponse()))
        }
        logger.info("responseProcessingTime: {}", responseProcessingTime)
    }
}

data class RequestContext(
    val receivedAt: Long,
    val socket: AsynchronousSocketChannel)

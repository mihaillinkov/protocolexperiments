@file:OptIn(ExperimentalTime::class)

package http

import http.ResponseCode.BAD_REQUEST
import http.ResponseCode.SERVER_ERROR
import http.handler.RequestHandler
import http.metrics.BatchMetricsService
import http.metrics.MetricData
import http.request.BadRequest
import http.request.RequestFactory
import http.request.RequestMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime


private val logger = LoggerFactory.getLogger(App::class.java)

class App(
    private val config: Config,
    private val batchMetricsService: BatchMetricsService? = null) {

    private val handlers = mutableMapOf<RequestKey, RequestHandler>()
    private val requestFactory = RequestFactory()

    fun addHandler(path: String, method: RequestMethod, handler: RequestHandler): App {
        return this.apply {
            handlers[RequestKey(path.lowercase(), method)] = handler
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    suspend fun start() = coroutineScope<Unit> {
        val processor = RequestProcessor(config, requestFactory, handlers)

        val serverChannel = AsynchronousServerSocketChannel.open().bind(InetSocketAddress(config.port), config.socketBacklogSize)
        logger.info("Application started on port {}, parallelRequestLimit: {}", config.port, config.parallelRequestLimit)

        serverChannel.use { server ->
            while (true) {
                val socket = server.acceptAwait()
                val receivedAt = Clock.System.now()

                launch(Dispatchers.Default) {
                    val processingDuration = measureTime {
                        processor.processRequest(socket)
                    }
                    batchMetricsService?.let {
                        val totalTime = Clock.System.now() - receivedAt

                        it.addMetric(MetricData("totalTime", totalTime.toDouble(DurationUnit.MILLISECONDS)))
                        it.addMetric(MetricData("processingTime", processingDuration.toDouble(DurationUnit.MILLISECONDS)))
                    }

                }
            }
        }
    }
}

data class RequestKey(
    val url: String,
    val method: RequestMethod)


class RequestProcessor(
    val config: Config,
    val requestFactory: RequestFactory,
    val handlers: Map<RequestKey, RequestHandler>) {

    suspend fun processRequest(socket: AsynchronousSocketChannel) {
        socket.use { socket ->
            val response = withTimeoutOrNull(config.requestTimeoutMs) {
                process(socket)
            }
            socket.writeAwait(buildHttpResponse(response ?: timeoutResponse()))
        }
    }

    private suspend fun process(socket: AsynchronousSocketChannel): HttpResponse {
        return try {
            val request = requestFactory.createRequest(socket)
            val handler = handlers[RequestKey(request.url, request.method)]

            handler?.handle(request) ?: HttpResponse(status = ResponseStatus.notFound())
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
}
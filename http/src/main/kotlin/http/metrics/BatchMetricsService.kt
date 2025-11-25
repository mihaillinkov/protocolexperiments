package http.metrics

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val logger = LoggerFactory.getLogger(BatchMetricsService::class.java)

private const val METRICS_CHANNEL_CAPACITY = 1000

class BatchMetricsService(
    private val metricsService: MetricsService,
    private val limit: Int = 500,
    private val interval: Duration = 60.seconds,
    private val requestScope: CoroutineScope) {

    private val metricsChannel = Channel<MetricData>(METRICS_CHANNEL_CAPACITY)

    suspend fun addMetric(metricData: MetricData) = coroutineScope {
        metricsChannel.send(metricData)
    }

    fun runProcessing(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                val metrics = bufferMetrics(limit, interval)

                requestScope.launch {
                    withTimeoutOrNull(timeout = 2.seconds) {
                        sendMetrics(metrics)
                    }
                }
            }
        }
    }

    private suspend fun bufferMetrics(limit: Int, interval: Duration): List<MetricData> {
        val metrics = mutableListOf<MetricData>()

        withTimeoutOrNull(interval) {
            for (metric in metricsChannel) {
                metrics.add(metric)

                if (metrics.size == limit) {
                    break
                }
            }
        }
        return metrics
    }

    private suspend fun sendMetrics(metrics: List<MetricData>) {
        try {
            metricsService.sendMetrics(metrics.map { it.toPoint() })
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (e: Exception) {
            logger.error("Can't send metrics", e)
        }
    }
}

private const val VALUE_FIELD = "value"

fun Point.value(value: Any): Point {
    return when (value) {
        is Number -> this.addField(VALUE_FIELD, value)
        else -> throw Error("Unsupported type")
    }
}

data class MetricData @OptIn(ExperimentalTime::class) constructor(
    val name: String,
    val value: Any,
    val tags: List<Pair<String, String>> = emptyList(),
    val timestamp: Instant = Clock.System.now())

@OptIn(ExperimentalTime::class)
fun MetricData.toPoint(): Point {
    return Point(name)
        .value(value)
        .addTags(tags.toMap())
        .time(timestamp.toEpochMilliseconds(), WritePrecision.MS)
}
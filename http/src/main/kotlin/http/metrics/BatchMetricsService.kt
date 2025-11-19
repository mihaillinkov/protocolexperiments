package http.metrics

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val logger = LoggerFactory.getLogger(BatchMetricsService::class.java)

class BatchMetricsService(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
    private val metricsService: MetricsService,
    private val limit: Int = 100,
    private val interval: Duration = 60.seconds) {

    private val metricsChannel = Channel<MetricData>(1000)

    suspend fun addMetric(metricData: MetricData) = coroutineScope {
        metricsChannel.send(metricData)
    }

    tailrec suspend fun process() {
        val buffer = mutableListOf<MetricData>()

        withContext<Unit>(dispatcher) {
            withTimeoutOrNull(interval) {
                for (metric in metricsChannel) {
                    buffer.add(metric)

                    if (buffer.size == limit) {
                        break
                    }
                }
            }
        }

        sendMetrics(buffer)

        process()
    }

    private suspend fun sendMetrics(metrics: MutableList<MetricData>) {
        try {
            metricsService.sendMetrics(metrics.map { it.toPoint() })
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
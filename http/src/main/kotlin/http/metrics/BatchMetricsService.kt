package http.metrics

import com.influxdb.client.write.Point
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class BatchMetricsService(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
    private val metricsService: MetricsService,
    private val limit: Int = 100,
    private val interval: Duration = 60.seconds) {

    private val metricsChannel = Channel<MetricData>()

    suspend fun addMetric(metricData: MetricData) = coroutineScope {
        metricsChannel.send(metricData)
    }

    suspend fun process() = withContext<Unit>(dispatcher) {
        val buffer = mutableListOf<MetricData>()

        launch {
            while (isActive) {
                delay(interval)
                metricsService.sendMetrics(buffer.map { it.toPoint() })
                buffer.clear()
            }
        }

        launch {
            for (metric in metricsChannel) {
                buffer.add(metric)
                if (buffer.size == limit) {
                    metricsService.sendMetrics(buffer.map { it.toPoint() })
                    buffer.clear()
                }
            }
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

data class MetricData(
    val name: String,
    val value: Any,
    val tags: List<Pair<String, String>> = emptyList())

fun MetricData.toPoint(): Point {
    return Point(name).value(value).addTags(tags.toMap())
}
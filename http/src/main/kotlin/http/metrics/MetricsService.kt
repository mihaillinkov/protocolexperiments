package http.metrics

import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.influxdb.client.write.Point
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(MetricsService::class.java)

class MetricsService(token: String) {

    private val client = InfluxDBClientKotlinFactory.create(
        url = "http://localhost:8086",
        token = token.toCharArray(),
        org = "test",
        bucket = "test-bucket"
    ).getWriteKotlinApi()

    suspend fun sendMetrics(points: List<Point>) {
        logger.info("Sending {} metrics", points.size)
        if (points.isNotEmpty()) {
            client.writePoints(points)
        }
    }
}
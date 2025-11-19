package http

import http.metrics.BatchMetricsService
import http.metrics.MetricsService
import http.request.RequestMethod
import kotlinx.coroutines.runBlocking

fun main(vararg args: String) = runBlocking {
    val config = buildConfig()

    val metricsUrl = System.getProperty("metrics.url", null)
    val metricsToken = System.getProperty("metrics.token", null)

    val batchMetricsService = if (metricsUrl != null && metricsToken != null) {
        val metricsService = MetricsService(metricsUrl, metricsToken)
        BatchMetricsService(metricsService = metricsService)
    } else {
        null
    }

    App(config, batchMetricsService)
        .addHandler(path ="/test", method = RequestMethod.GET) {
            HttpResponse(ResponseStatus.ok(), "Test \uD83D\uDC24".toByteArray(Charsets.UTF_8))
        }
        .start()
}

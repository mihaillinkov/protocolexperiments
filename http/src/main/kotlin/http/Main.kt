package http

import http.metrics.BatchMetricsService
import http.metrics.MetricsService
import http.request.RequestMethod
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(vararg args: String) = runBlocking {
    val config = buildConfig()

    val batchMetricsService = args.getOrNull(0)?.let { address ->
        val metricsService = MetricsService(address)
        BatchMetricsService(metricsService = metricsService)
    }

    launch {
        batchMetricsService?.process()
    }

    App(config, batchMetricsService)
        .addHandler(path ="/test", method = RequestMethod.GET) {
            HttpResponse(ResponseStatus.ok(), "Test \uD83D\uDC24".toByteArray(Charsets.UTF_8))
        }
        .start()
}

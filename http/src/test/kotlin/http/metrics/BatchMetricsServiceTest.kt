package http.metrics

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.engine.coroutines.backgroundScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalKotest::class, ExperimentalTime::class)
class BatchMetricsServiceTest: FunSpec({
    lateinit var metricsService: MetricsService
    lateinit var batchService: BatchMetricsService

    context("BatchServiceMetrics test").config(coroutineTestScope = true) {
        beforeTest {
            metricsService = mockk<MetricsService>()
            coEvery { metricsService.sendMetrics(any()) }
                .coAnswers {
                    delay(1.seconds) }
                .coAndThen {
                    delay(3.seconds) }
                .coAndThen {
                    delay(1.seconds) }

            println(backgroundScope)
            batchService = BatchMetricsService(
                metricsService = metricsService,
                limit = 10,
                interval = 10.seconds,
                requestScope = backgroundScope,
                timeout = 2.seconds)
        }

        test("test").config(coroutineTestScope = true) {
            batchService.runProcessing(backgroundScope)

            testCoroutineScheduler.advanceTimeBy(10.seconds)
            testCoroutineScheduler.runCurrent()

            repeat(batchService.limit) {
                batchService.addMetric(MetricData("test", 3.0))
            }

            testCoroutineScheduler.advanceTimeBy(10.seconds)
            testCoroutineScheduler.runCurrent()

            coVerify(exactly = 3) { metricsService.sendMetrics(any()) }
        }
    }
})
package http

import http.request.RequestMethod
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val PORT = 8080
private const val BASE_URL = "http://localhost:${PORT}"

private val TEST_TIMEOUT = 300.milliseconds

class AppTests: FunSpec({
    lateinit var client: HttpClient
    lateinit var appJob: Job
    lateinit var scope: CoroutineScope

    context("App test") {

        beforeTest {
            scope = CoroutineScope(Dispatchers.Default)
            val config = Config(
                port = PORT, requestTimeoutMs = 200, parallelRequestLimit = 16, socketBacklogSize = 50)
            appJob = launch {
                App(config, requestProcessorScope = scope)
                    .addHandler(path = "/test", method = RequestMethod.GET) {
                        HttpResponse(
                            status = ResponseStatus.ok(),
                            body = "test-result".toByteArray(Charsets.UTF_8))
                    }
                    .addHandler(path = "/long-request", method = RequestMethod.GET) {
                        delay(5000)
                        HttpResponse(ResponseStatus.ok())
                    }
                    .startProcessing()
            }

            client = HttpClient()
        }

        afterTest {
            appJob.cancelAndJoin()
            scope.cancel()
            client.close()
        }

        withData(listOf("test", "Test", "TEST", "tEST")) { path ->
            val response = client.get("$BASE_URL/$path")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldBe "test-result"
        }

        test("should timeout when content-length is greater than body length").config(timeout = TEST_TIMEOUT) {
            val response = client.get("$BASE_URL/test") {
                headers.append("content-length", "1")
            }

            response.status shouldBe HttpStatusCode.RequestTimeout
        }

        test("/not-found endpoint should return 404 NotFound").config(timeout = TEST_TIMEOUT) {
            val response = client.get("$BASE_URL/not-found")
            response.status shouldBe HttpStatusCode.NotFound
        }

        test("/long-request should return 408 RequestTimeout").config(timeout = TEST_TIMEOUT) {
            val response = client.get("$BASE_URL/long-request")
            response.status shouldBe HttpStatusCode.RequestTimeout
        }
    }
})

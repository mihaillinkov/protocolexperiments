package http

import http.request.RequestMethod
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppTests: FunSpec() {
    lateinit var client: HttpClient
    lateinit var appJob: Job

    init {
        context("App test") {
            beforeTest {
                appJob = launch {
                    App(Config(requestTimeoutMs = 100))
                        .addHandler(path = "/test", method = RequestMethod.GET) {
                            HttpResponse(
                                status = ResponseStatus.ok(),
                                body = "test-result".toByteArray(Charsets.UTF_8))
                        }
                        .addHandler(path = "/long-request", method = RequestMethod.GET) {
                            delay(5000)
                            HttpResponse(ResponseStatus.ok())
                        }
                        .start()
                }

                client = HttpClient()
            }

            afterTest {
                appJob.cancelAndJoin()
                client.close()
            }

            test("/test endpoint should return 200 OK") {
                val response = client.get("http://localhost:8080/test")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "test-result"
            }

            test("/not-found endpoint should return 404 NotFound") {
                val response = client.get("http://localhost:8080/not-found")
                response.status shouldBe HttpStatusCode.NotFound
            }

            test("/long-request should return 408 RequestTimeout") {
                val response = client.get("http://localhost:8080/long-request")
                response.status shouldBe HttpStatusCode.RequestTimeout
            }
        }
    }
}

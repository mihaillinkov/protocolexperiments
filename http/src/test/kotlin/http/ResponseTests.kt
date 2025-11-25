package http

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEqualIgnoringCase

private const val CONTENT_LENGTH_HEADER = "Content-Length"
private const val CONTENT_TYPE_HEADER = "Content-type: text/plain; charset=utf-8"
private const val CONNECTION_CLOSE_HEADER = "Connection: Close"
private const val END_LINE = "\r\n"

class ResponseTests: FunSpec({

    test("timeout response should have status 408") {
        val response = timeoutResponse()

        response.body shouldBe null
        response.status shouldBe ResponseStatus(408, "Request Timeout")
        response.headers should beEmpty()
    }

    test("contentLengthHeader should return valid content-length header") {
        contentLengthHeader(101) shouldBeEqualIgnoringCase "content-length: 101"
    }

    test("buildResponseStartLine should include message when not null") {
        val startLine = buildResponseStartLine(ResponseStatus(200, "Ok"))

        startLine shouldBe "HTTP/1.1 200 Ok"
    }

    test("buildResponseStartLine should not include message when null") {
        val startLine = buildResponseStartLine(ResponseStatus(200, null))

        startLine shouldBe "HTTP/1.1 200"
    }

    test("buildHttpResponse test when body is null") {
        val httpResponse = HttpResponse(
            status = ResponseStatus(200, "OK"),
            body = null)
        val response = buildHttpResponse(httpResponse)

        response shouldBe ("HTTP/1.1 200 OK$END_LINE" +
                "$CONTENT_LENGTH_HEADER: 0$END_LINE" +
                "$CONTENT_TYPE_HEADER$END_LINE" +
                "$CONNECTION_CLOSE_HEADER$END_LINE$END_LINE").toByteArray()
    }

    test("buildHttpResponse test when body is not null") {
        val httpResponse = HttpResponse(
            status = ResponseStatus(200, "OK"),
            body = "Hello \uD83D\uDC25".toByteArray(Charsets.UTF_8),
            headers = listOf("test-header: test"))
        val response = buildHttpResponse(httpResponse)

        response shouldBe ("HTTP/1.1 200 OK$END_LINE" +
                "test-header: test$END_LINE" +
                "$CONTENT_LENGTH_HEADER: 10$END_LINE" +
                "$CONTENT_TYPE_HEADER$END_LINE" +
                "$CONNECTION_CLOSE_HEADER$END_LINE" +
                END_LINE +
                "Hello \uD83D\uDC25").toByteArray(Charsets.UTF_8)
    }

    test("ResponseStatus.ok test") {
        ResponseStatus.ok() shouldBe ResponseStatus(200, "OK")
    }

    test("ResponseStatus.notFound test") {
        ResponseStatus.notFound() shouldBe ResponseStatus(404, "NOT_FOUND")
    }
})
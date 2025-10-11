package http

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEqualIgnoringCase

class ResponseTests: FunSpec() {
    init {
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
    }
}
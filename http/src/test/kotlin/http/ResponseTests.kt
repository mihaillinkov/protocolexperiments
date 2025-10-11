package http

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class ResponseTests: FunSpec({
    test("timeout response should have status 408") {
        val response = timeoutResponse()

        response.body shouldBe null
        response.status shouldBe ResponseStatus(408, "Request Timeout")
        response.headers should beEmpty()
    }
})
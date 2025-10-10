package http

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ResponseTests {
    @Test
    fun testTimeoutResponse() {
        val response = timeoutResponse()

        Assertions.assertNull(response.body)
        Assertions.assertEquals(ResponseStatus(408, "Request Timeout"), response.status)
    }
}
package http

import http.mock.mockSocketChannelRead
import http.request.BadRequest
import http.request.RequestMethod.DELETE
import http.request.RequestMethod.GET
import http.request.RequestMethod.POST
import http.request.RequestMethod.PUT
import http.request.Header
import http.request.RequestFactory
import http.request.getMethod
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import java.nio.channels.AsynchronousSocketChannel


class RequestTest: FunSpec({
    lateinit var socketChannel: AsynchronousSocketChannel
    val requestFactory = RequestFactory(readBufferCapacity = 10)

    beforeTest {
        socketChannel = mockk()
        mockkStatic(socketChannel::readAwait)
    }

    afterTest {
        unmockkStatic(socketChannel::readAwait)
    }

    test("buildRequestObject test") {
        val body = "test"

        mockSocketChannelRead(socketChannel, ("GET /test HTTP/1.1\r\n" +
                "test-header-1: test-1\r\n" +
                "Content-Length: 4\r\n\r\n" +
                body).toByteArray(Charsets.UTF_8))

        val request = requestFactory.createRequest(socketChannel)

        request.method shouldBe GET
        request.url shouldBe "/test"
        request.body shouldBe body.toByteArray(Charsets.UTF_8)
        request.headers shouldContainExactlyInAnyOrder listOf(
            Header("content-length", "4"), Header("test-header-1", "test-1"))

        coVerify(exactly = 7) { socketChannel.readAwait(any()) }
    }

    test("buildRequestObject should throw BadRequest when unsupported request method") {
        mockSocketChannelRead(socketChannel, "FAILED-METHOD /test HTTP/1.1\r\n\r\n".toByteArray(Charsets.UTF_8))

        shouldThrow<BadRequest> { requestFactory.createRequest(socketChannel) }
            .message shouldBe "Unsupported http method FAILED-METHOD, should be one of [GET, POST, PUT, DELETE]"
    }

    test("buildRequestObject should throw BadRequest when invalid startline") {
        val invalidStartLine = "GET HTTP/1.1"
        mockSocketChannelRead(socketChannel, "$invalidStartLine\r\n\r\n".toByteArray(Charsets.UTF_8))

        shouldThrow<BadRequest> { requestFactory.createRequest(socketChannel) }
            .message shouldBe "Invalid startline $invalidStartLine"
    }

    test("buildRequestObject should throw BadRequest when stream ends") {
        mockSocketChannelRead(socketChannel, "GET /test HTTP/1.1".toByteArray(Charsets.UTF_8))

        shouldThrow<BadRequest> {
            requestFactory.createRequest(socketChannel)
        }
    }

    context("getMethod test should return RequestMethod when valid") {
        withData(
            "get" to GET,
            "GET" to GET,
            "GeT" to GET,
            "POSt" to POST,
            "DELETE" to DELETE,
            "PUT" to PUT) { (method, expected) ->

            val actualMethod = getMethod(method)
            actualMethod shouldBe expected
        }
    }

    context("getMethod test should return null when invalid method name") {
        withData("OPTION", "GETT", "Test", "INVALID") {
            method -> getMethod(method) shouldBe null
        }
    }
})
package http

import http.RequestMethod.DELETE
import http.RequestMethod.GET
import http.RequestMethod.POST
import http.RequestMethod.PUT
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler


class RequestTest: FunSpec() {
    lateinit var socketChannel: AsynchronousSocketChannel

    init {
        beforeTest {
            socketChannel = mockk()
            mockkStatic(socketChannel::readAwait)
        }

        afterTest {
            unmockkStatic(socketChannel::readAwait)
        }

        test("readLine test") {
            val output = "GET /test HTTP/1.1\r\n".toByteArray(Charsets.UTF_8).map { byteArrayOf(it) }
            coEvery { socketChannel.readAwait() } returnsMany output

            val result = readLine(socketChannel)

            result shouldBe "GET /test HTTP/1.1".toByteArray(Charsets.UTF_8)
        }

        test("readLine should throw CancellationException when readAwait returns null") {
            coEvery { socketChannel.readAwait() } returns null

            shouldThrow<CancellationException> { readLine(socketChannel) }
        }

        test("readBody test should throw CancellationException when readAwait returns null") {
            coEvery { socketChannel.readAwait(any<Int>()) } returns null

            shouldThrow<CancellationException> { readBody(socketChannel, 1024) }
        }

        test("readBody test") {
            val output = "test data".toByteArray(Charsets.UTF_8).map { byteArrayOf(it) }
            coEvery { socketChannel.readAwait(any<Int>()) } returnsMany(output)

            readBody(socketChannel, 9) shouldBe "test data".toByteArray(Charsets.UTF_8)
        }

        test("buildRequestObject test") {
            val body = "test"

            mockSocketChannelRead(socketChannel, ("GET /test HTTP/1.1\r\n" +
                    "test-header-1: test-1\r\n" +
                    "Content-Length: 4\r\n\r\n" +
                    body).toByteArray(Charsets.UTF_8))

            val request = buildRequestObject(socketChannel)

            request.method shouldBe GET
            request.url shouldBe "/test"
            request.body shouldBe body.toByteArray(Charsets.UTF_8)
            request.headers shouldContainExactlyInAnyOrder listOf("Content-Length: 4", "test-header-1: test-1")
        }

        test("buildRequestObject should throw BadRequest when unsupported request method") {
            mockSocketChannelRead(socketChannel, "FAILED-METHOD /test HTTP/1.1\r\n\r\n".toByteArray(Charsets.UTF_8))

            shouldThrow<BadRequest> { buildRequestObject(socketChannel) }
                .message shouldBe "Unsupported http method FAILED-METHOD, should be one of [GET, POST, PUT, DELETE]"
        }

        test("buildRequestObject should throw BadRequest when invalid startline") {
            val invalidStartLine = "GET HTTP/1.1"
            mockSocketChannelRead(socketChannel, "$invalidStartLine\r\n\r\n".toByteArray(Charsets.UTF_8))

            shouldThrow<BadRequest> { buildRequestObject(socketChannel) }
                .message shouldBe "Invalid startline $invalidStartLine"
        }

        test("buildRequestObject should throw CancellationException when stream ends") {
            mockSocketChannelRead(socketChannel, "GET /test HTTP/1.1".toByteArray(Charsets.UTF_8))

            shouldThrow<CancellationException> {
                buildRequestObject(socketChannel)
            }
        }

        context("getMethod test should return RequestMethod when valid") {
            withData("get", "GET", "GeT", "POSt", "DELETE", "PUT") { method ->
                val actualMethod = getMethod(method)
                actualMethod shouldNotBe null
                actualMethod shouldBeOneOf listOf(POST, GET, PUT, DELETE)
            }
        }

        context("getMethod test should parse") {
            withData("OPTION", "GETT", "Test", "INVALID") {
                method -> getMethod(method) shouldBe null
            }
        }
    }
}

fun mockSocketChannelRead(socketChannel: AsynchronousSocketChannel, bytes: ByteArray) {
    val mockFunc = readMock(bytes)

    every {
        socketChannel.read(any<ByteBuffer>(), anyNullable<Any?>(), any<CompletionHandler<Int, Any?>>())
    } answers { (_, scope) ->
        val (args1, _, args3) = scope.args
        val buffer = args1 as ByteBuffer
        val handler = args3 as CompletionHandler<Int, Any?>

        mockFunc(buffer, handler)
    }
}

fun readMock(bytes: ByteArray): (ByteBuffer, CompletionHandler<Int, Any?>) -> Unit {
    var offset = 0

    return { buffer, completionHandler ->
        val bufferSize = buffer.limit()

        val bytesToAddSize = minOf(bufferSize, bytes.size - offset)

        val bytesToAdd = bytes.sliceArray(offset until  offset + bytesToAddSize)
        buffer.put(bytesToAdd)

        offset += bytesToAddSize

        if (bytesToAddSize == 0) {
            completionHandler.completed(-1, null)
        } else {
            completionHandler.completed(bytesToAddSize, null)
        }
    }
}

package http

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import java.nio.channels.AsynchronousSocketChannel


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
    }
}
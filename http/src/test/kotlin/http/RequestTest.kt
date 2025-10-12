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

        test("should read line") {
            val output = "GET /test HTTP/1.1\r\n".toByteArray(Charsets.UTF_8).map { byteArrayOf(it) }
            coEvery { socketChannel.readAwait() } returnsMany output

            val result = readLine(socketChannel)

            result shouldBe "GET /test HTTP/1.1".toByteArray(Charsets.UTF_8)
        }

        test("should throw CancellationException when ") {
            coEvery { socketChannel.readAwait() } returns null

            shouldThrow<CancellationException> { readLine(socketChannel) }
        }
    }
}
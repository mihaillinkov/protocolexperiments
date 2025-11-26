package http

import http.mock.mockSocketChannelRead
import http.request.BadRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.nio.channels.AsynchronousSocketChannel

class ByteStreamTest: FunSpec({
    context("ByteStream tests") {
        test("test next()") {
            val channel = mockk<AsynchronousSocketChannel>()

            mockSocketChannelRead(
                socketChannel = channel,
                bytes = "test".toByteArray())

            val byteStream = createByteStream(channel, 10)

            byteStream.next() shouldBe 116
            byteStream.next() shouldBe 101
            byteStream.next() shouldBe 115
            byteStream.next() shouldBe 116

            shouldThrow<BadRequest> { byteStream.next() }
        }

        test("test next(n)") {
            val channel = mockk<AsynchronousSocketChannel>()

            mockSocketChannelRead(
                socketChannel = channel,
                bytes = "test".toByteArray())

            val byteStream = createByteStream(channel, 10)

            byteStream.next(3) shouldBe byteArrayOf(116, 101, 115)
            shouldThrow<BadRequest> { byteStream.next(3) }
        }
    }
})
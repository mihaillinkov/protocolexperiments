package http.mock

import io.mockk.every
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3

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

private fun readMock(bytes: ByteArray): (ByteBuffer, CompletionHandler<Int, Any?>) -> Unit {
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

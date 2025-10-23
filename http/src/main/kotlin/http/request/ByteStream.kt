package http.request

import http.readAwait
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

private const val BUFFER_CAPACITY = 128

interface ByteStream: Closeable {
    suspend fun next(): Byte
}

fun createByteStream(channel: AsynchronousSocketChannel) = object: ByteStream {
    private val buffer = ByteBuffer.allocate(BUFFER_CAPACITY).limit(0)

    override tailrec suspend fun next(): Byte {
        if (buffer.hasRemaining()) {
            return buffer.get()
        }
        channel.readAwait(buffer)
        return next()
    }

    override fun close() {
        channel.close()
    }
}
package http

import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

private const val BUFFER_CAPACITY = 128
private val LINE_BREAK = "\r\n".toByteArray()

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

internal suspend fun ByteStream.readLine(): ByteArray {
    val bytes = mutableListOf<Byte>()

    while (bytes.size < 2 || !(bytes[bytes.lastIndex - 1] == LINE_BREAK[0] && bytes[bytes.lastIndex] == LINE_BREAK[1])) {
        bytes.add(next())
    }

    return bytes.dropLast(2).toByteArray()
}

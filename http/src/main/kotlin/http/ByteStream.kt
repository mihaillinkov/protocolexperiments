package http

import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

private const val BUFFER_CAPACITY = 256
private const val INITIAL_BUFFER_CAPACITY = 20
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
    val bytes = ArrayList<Byte>(INITIAL_BUFFER_CAPACITY)
    var preLast: Byte = -1
    do {
        bytes.add(next())
        if (preLast == LINE_BREAK[0] && bytes.last() == LINE_BREAK[1]) {
            break
        }
        preLast = bytes.last()
    } while (true)

    bytes.removeLast()
    bytes.removeLast()
    return bytes.toByteArray()
}

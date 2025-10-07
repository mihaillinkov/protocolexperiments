package http

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val BYTE_BUFFER_CAPACITY = 256

fun <T, R> completionHandler(
    continuation: CancellableContinuation<R>,
    completed: (T, Any?) -> Unit) = object: CompletionHandler<T, Any?> {

    override fun completed(result: T, attachment: Any?) {
        completed(result, attachment)
    }

    override fun failed(e: Throwable, attachment: Any?) {
        continuation.resumeWithException(e)
    }
}

suspend fun AsynchronousServerSocketChannel.acceptAwait() = suspendCancellableCoroutine { continuation ->
    val handler = completionHandler<AsynchronousSocketChannel, AsynchronousSocketChannel>(continuation) { result, _ ->
        continuation.resume(result)
    }

    this.accept(null, handler)
}

suspend fun AsynchronousSocketChannel.writeAwait(bytes: ByteArray) = suspendCancellableCoroutine { continuation ->
    val handler = completionHandler<Int, Unit>(continuation) { _, _ ->
        continuation.resume(Unit)
    }

    this.write(ByteBuffer.wrap(bytes), null, handler)
}

suspend fun AsynchronousSocketChannel.readAwait() = suspendCancellableCoroutine { continuation ->
    val buffer = ByteBuffer.allocate(BYTE_BUFFER_CAPACITY)

    val handler = completionHandler<Int, ByteArray?>(continuation) { size, _ ->
        val result = if (size == -1) {
            null
        } else {
            buffer.flip()
            val bytes = ByteArray(size)
            buffer.get(bytes)
            bytes
        }
        continuation.resume(result)
    }

    read(buffer, null, handler)
}
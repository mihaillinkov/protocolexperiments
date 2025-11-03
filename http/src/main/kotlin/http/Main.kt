package http

import http.request.RequestMethod
import kotlinx.coroutines.runBlocking

private const val DEFAULT_PORT = 8080
private const val SOCKET_BACKLOG_SIZE = 1024

fun main(vararg args: String) = runBlocking {
    val port = args.getOrNull(0)?.toInt() ?: DEFAULT_PORT

    val config = Config(
        port = port,
        requestTimeoutMs = 2000,
        maxParallelRequest = 15,
        socketBacklogSize = SOCKET_BACKLOG_SIZE)

    App(config)
        .addHandler(path ="/test", method = RequestMethod.GET) {
            HttpResponse(ResponseStatus.ok(), "Test \uD83D\uDC24".toByteArray(Charsets.UTF_8))
        }
        .start()
}

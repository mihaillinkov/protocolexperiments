package http

private const val DEFAULT_PORT = 8080
private const val DEFAULT_REQUEST_TIMEOUT = 2000
private const val DEFAULT_PARALLEL_REQUEST_LIMIT = 15
private const val DEFAULT_SOCKET_BACKLOG_LIMIT = 50

fun buildConfig(
    port: Int? = null,
    requestTimeoutInMs: Int? = null,
    parallelRequestLimit: Int? = null,
    socketBacklogSize: Int? = null): Config {

    val port = port ?: Integer.getInteger("port", DEFAULT_PORT)
    val timeout = requestTimeoutInMs ?: Integer.getInteger("request.timeout", DEFAULT_REQUEST_TIMEOUT)
    val parallelRequestLimit = parallelRequestLimit ?: Integer.getInteger("parallel.request.limit", DEFAULT_PARALLEL_REQUEST_LIMIT)
    val socketBacklogSize = socketBacklogSize ?: Integer.getInteger("socket.backlog.size", DEFAULT_SOCKET_BACKLOG_LIMIT)

    return Config(
        port = port,
        requestTimeoutMs = timeout.toLong(),
        parallelRequestLimit = parallelRequestLimit,
        socketBacklogSize = socketBacklogSize)
}

data class Config(
    val port: Int,
    val requestTimeoutMs: Long,
    val parallelRequestLimit: Int,
    val socketBacklogSize: Int)

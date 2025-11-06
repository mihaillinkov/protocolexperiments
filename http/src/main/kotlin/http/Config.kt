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

    val port = port ?: Integer.getInteger("PORT", DEFAULT_PORT)
    val timeout = requestTimeoutInMs ?: Integer.getInteger("REQUEST_TIMEOUT", DEFAULT_REQUEST_TIMEOUT)
    val parallelRequestLimit = parallelRequestLimit ?: Integer.getInteger("PARALLEL_REQUEST_LIMIT", DEFAULT_PARALLEL_REQUEST_LIMIT)
    val socketBacklogSize = socketBacklogSize ?: Integer.getInteger("SOCKET_BACKLOG_LIMIT", DEFAULT_SOCKET_BACKLOG_LIMIT)

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

/*
java -DPARALLEL_REQUEST_LIMIT=4 -Dcom.sun.management.jmxremote  -Dcom.sun.management.jmxremote.port=9010  -Dcom.sun.management.jmxremote.rmi.port=9010 -Dcom.sun.management.jmxremote.ssl=false  -Dcom.sun.management.jmxremote.authenticate=false  -Djava.rmi.server.hostnamame=192.168.1.200 -cp "./*" http.MainKt
 */
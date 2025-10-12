package http

data class Config(
    val port: Int = 8080,
    val parallelProcessors: Int = 1,
    val requestTimeoutMs: Long = 1000)
package http

data class Config(
    val port: Int = 8080,
    val requestTimeoutMs: Long = 1000,
    val maxParallelRequest: Int = 1,
    val socketBacklogSize: Int = 1024)
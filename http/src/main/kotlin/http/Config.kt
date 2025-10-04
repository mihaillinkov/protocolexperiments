package http

data class Config(
    val port: Int = 8008,
    val parallelProcessors: Int = 2)
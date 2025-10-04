package http

import kotlinx.coroutines.runBlocking

private const val DEFAULT_PORT = 8080

fun main(vararg args: String) = runBlocking {
    val port = args.getOrNull(0)?.toInt() ?: DEFAULT_PORT

    App(Config(port, 2, 2000)).run()
}

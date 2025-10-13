package http

import kotlinx.coroutines.runBlocking

private const val DEFAULT_PORT = 8080

fun main(vararg args: String) = runBlocking {
    val port = args.getOrNull(0)?.toInt() ?: DEFAULT_PORT

    App(Config(port, 1, 2000))
        .addHandler(path ="/test", method = RequestMethod.GET) {
            HttpResponse(ResponseStatus.ok(), "Test \uD83D\uDC24".toByteArray(Charsets.UTF_8))
        }
        .run()
}

package http

import http.request.RequestMethod
import kotlinx.coroutines.runBlocking

fun main(vararg args: String) = runBlocking {
    val config = buildConfig()

    App(config)
        .addHandler(path ="/test", method = RequestMethod.GET) {
            HttpResponse(ResponseStatus.ok(), "Test \uD83D\uDC24".toByteArray(Charsets.UTF_8))
        }
        .start()
}

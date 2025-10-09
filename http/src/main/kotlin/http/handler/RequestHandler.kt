package http.handler

import http.HttpResponse
import http.HttpRequest

fun interface RequestHandler {
    fun handle(request: HttpRequest): HttpResponse
}